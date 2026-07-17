import { readFile, readdir } from "node:fs/promises";
import { extname, join } from "node:path";

const token = process.env.GOOGLE_OAUTH_ACCESS_TOKEN;
const packageName = process.env.PLAY_PACKAGE_NAME || "com.aria.assistant";
const apiRoot = "https://androidpublisher.googleapis.com/androidpublisher/v3";
const promoVideo = "https://youtu.be/_wgi6OSL80g";

if (!token) throw new Error("GOOGLE_OAUTH_ACCESS_TOKEN is required");

async function request(url, options = {}, expected = [200]) {
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(options.body && typeof options.body === "string"
        ? { "Content-Type": "application/json" }
        : {}),
      ...options.headers,
    },
  });
  const text = await response.text();
  if (!expected.includes(response.status)) {
    let message = text;
    try { message = JSON.parse(text)?.error?.message || text; } catch {}
    throw new Error(`${options.method || "GET"} ${url} failed (${response.status}): ${message}`);
  }
  return text ? JSON.parse(text) : null;
}

function money(units, nanos = 0) {
  return { currencyCode: "USD", units: String(units), nanos };
}

async function convertedPrices(price) {
  return request(`${apiRoot}/applications/${packageName}/pricing:convertRegionPrices`, {
    method: "POST",
    body: JSON.stringify({ price }),
  });
}

async function ensureSubscription(definition) {
  const resourceUrl = `${apiRoot}/applications/${packageName}/subscriptions/${definition.productId}`;
  let existing;
  try {
    existing = await request(resourceUrl);
  } catch (error) {
    if (!String(error).includes("(404)")) throw error;
  }

  if (!existing) {
    const conversion = await convertedPrices(definition.price);
    const regionalConfigs = Object.values(conversion.convertedRegionPrices).map((entry) => ({
      regionCode: entry.regionCode,
      newSubscriberAvailability: true,
      price: entry.price,
    }));
    const subscription = {
      packageName,
      productId: definition.productId,
      listings: [{
        languageCode: "en-US",
        title: definition.title,
        description: definition.description,
        benefits: definition.benefits,
      }],
      basePlans: [{
        basePlanId: definition.basePlanId,
        regionalConfigs,
        otherRegionsConfig: {
          ...conversion.convertedOtherRegionsPrice,
          newSubscriberAvailability: true,
        },
        autoRenewingBasePlanType: {
          billingPeriodDuration: definition.period,
          resubscribeState: "RESUBSCRIBE_STATE_ACTIVE",
        },
      }],
    };
    const regionsVersion = encodeURIComponent(conversion.regionVersion.version);
    const createUrl = `${apiRoot}/applications/${packageName}/subscriptions?productId=${definition.productId}&regionsVersion.version=${regionsVersion}`;
    existing = await request(createUrl, { method: "POST", body: JSON.stringify(subscription) });
    console.log(`Created subscription ${definition.productId}`);
  } else {
    console.log(`Subscription ${definition.productId} already exists; preserving its configuration`);
  }

  const plan = existing.basePlans?.find((item) => item.basePlanId === definition.basePlanId);
  if (!plan) throw new Error(`${definition.productId} exists without expected base plan ${definition.basePlanId}`);
  if (plan.state === "DRAFT" || plan.state === "INACTIVE") {
    await request(`${resourceUrl}/basePlans/${definition.basePlanId}:activate`, {
      method: "POST",
      body: JSON.stringify({ packageName, productId: definition.productId, basePlanId: definition.basePlanId }),
    });
    console.log(`Activated base plan ${definition.basePlanId}`);
  } else if (plan.state !== "ACTIVE") {
    throw new Error(`Unexpected state ${plan.state} for ${definition.productId}/${definition.basePlanId}`);
  }
}

async function uploadImage(editId, imageType, path) {
  const bytes = await readFile(path);
  const mime = extname(path).toLowerCase() === ".jpg" ? "image/jpeg" : "image/png";
  const url = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${packageName}/edits/${editId}/listings/en-US/${imageType}?uploadType=media`;
  await request(url, { method: "POST", body: bytes, headers: { "Content-Type": mime } });
  console.log(`Uploaded ${imageType}: ${path}`);
}

async function configureListing() {
  const title = (await readFile("play-store/store-listing/title.txt", "utf8")).trim();
  const shortDescription = (await readFile("play-store/store-listing/short-description.txt", "utf8")).trim();
  const fullDescription = (await readFile("play-store/store-listing/full-description.txt", "utf8")).trim();
  const edit = await request(`${apiRoot}/applications/${packageName}/edits`, {
    method: "POST",
    body: "{}",
  });
  const base = `${apiRoot}/applications/${packageName}/edits/${edit.id}/listings/en-US`;
  await request(base, {
    method: "PUT",
    body: JSON.stringify({ language: "en-US", title, shortDescription, fullDescription, video: promoVideo }),
  });

  const imageSets = [
    ["icon", ["play-store/graphics/icon/aria-play-icon-512.png"]],
    ["featureGraphic", ["play-store/graphics/feature-graphic/aria-feature-graphic-1024x500.png"]],
    ["phoneScreenshots", (await readdir("play-store/graphics/phone-screenshots/framed"))
      .filter((name) => /\.(png|jpe?g)$/i.test(name)).sort()
      .map((name) => join("play-store/graphics/phone-screenshots/framed", name))],
  ];
  if (imageSets[2][1].length < 2) throw new Error("At least two framed phone screenshots are required");

  for (const [type, files] of imageSets) {
    await request(`${base}/${type}`, { method: "DELETE" }, [200, 204]);
    for (const file of files) await uploadImage(edit.id, type, file);
  }

  await request(`${apiRoot}/applications/${packageName}/edits/${edit.id}:validate`, {
    method: "POST",
    body: "{}",
  });
  await request(`${apiRoot}/applications/${packageName}/edits/${edit.id}:commit?changesNotSentForReview=true&changesInReviewBehavior=ERROR_IF_IN_REVIEW`, {
    method: "POST",
    body: "{}",
  });
  console.log("Committed English listing and artwork without requesting public rollout");
}

await ensureSubscription({
  productId: "aria_premium_monthly",
  basePlanId: "monthly",
  period: "P1M",
  price: money(2, 990_000_000),
  title: "Aria Premium Monthly",
  description: "Monthly access to Aria Premium features.",
  benefits: ["Premium on-device AI model", "Additional voices", "Advanced assistant tools"],
});
await ensureSubscription({
  productId: "aria_premium_yearly",
  basePlanId: "yearly",
  period: "P1Y",
  price: money(19, 990_000_000),
  title: "Aria Premium Yearly",
  description: "Yearly access to Aria Premium features at the best value.",
  benefits: ["Premium on-device AI model", "Additional voices", "Advanced assistant tools"],
});
await configureListing();
