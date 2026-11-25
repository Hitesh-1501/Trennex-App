const STOP_WORDS = new Set(['a', 'an', 'and', 'are', 'best', 'do', 'for', 'have', 'i', 'in', 'is', 'me', 'of', 'on', 'or', 'show', 'the', 'to', 'what', 'with']);

function validateQuestion(value) {
  if (typeof value !== 'string') return null;
  const question = value.trim().replace(/\s+/g, ' ');
  return question.length >= 3 && question.length <= 500 ? question : null;
}

function formatPrice(minor) {
  const amount = Number(minor);
  if (!Number.isSafeInteger(amount) || amount < 0) return 'Price unavailable';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' })
    .format(amount / 100);
}

function selectProducts(question, products) {
  const terms = question.toLowerCase().match(/[\p{L}\p{N}]+/gu)
    ?.filter((word) => word.length > 1 && !STOP_WORDS.has(word)) || [];
  if (!terms.length) return [];
  return products.map((product) => {
    const name = String(product.name || '').toLowerCase();
    const detail = [product.description, product.shortDescription, product.categoryId,
      ...(Array.isArray(product.tags) ? product.tags : [])].join(' ').toLowerCase();
    const score = terms.reduce((sum, term) => sum + (name.includes(term) ? 3 : 0)
      + (detail.includes(term) ? 1 : 0), 0);
    return { ...product, score };
  }).filter((product) => product.score > 0)
    .sort((a, b) => b.score - a.score || a.name.localeCompare(b.name))
    .slice(0, 5);
}

function parseGroundedAnswer(raw, allowedProducts) {
  let parsed;
  try {
    parsed = JSON.parse(String(raw).trim().replace(/^```(?:json)?\s*|\s*```$/g, ''));
  } catch {
    throw new Error('The assistant response was not valid JSON.');
  }
  if (typeof parsed.answer !== 'string' || !parsed.answer.trim()
      || parsed.answer.length > 1200 || !Array.isArray(parsed.productIds)) {
    throw new Error('The assistant response had an invalid shape.');
  }
  const allowed = new Map(allowedProducts.map((product) => [product.id, product]));
  const ids = [...new Set(parsed.productIds.filter((id) => typeof id === 'string'))].slice(0, 3);
  const linkedProducts = ids.map((id) => allowed.get(id)).filter(Boolean);
  const knownPrices = linkedProducts.map((product) => formatPrice(product.minPriceMinor));
  const unverifiedAmount = [...parsed.answer.matchAll(/\d[\d,]{3,}(?:\.\d{1,2})?/g)]
    .some(([amount]) => !knownPrices.some((price) => price.includes(amount)));
  return {
    answer: unverifiedAmount
      ? 'I found these matching products. Check their current prices and details below.'
      : parsed.answer.trim(),
    products: linkedProducts.map((product) => ({
      id: product.id,
      legacyId: product.legacyId,
      name: product.name,
      priceMinor: product.minPriceMinor,
      imageUrl: product.thumbnail?.url || '',
    })),
  };
}

module.exports = { validateQuestion, formatPrice, selectProducts, parseGroundedAnswer };
