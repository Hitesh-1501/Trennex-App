const test = require('node:test');
const assert = require('node:assert/strict');
const { validateQuestion, formatPrice, selectProducts, parseGroundedAnswer } = require('./assistant-core');

const products = [
  { id: 'phone', legacyId: 10, name: 'Samsung Galaxy Phone', categoryId: 'electronics',
    shortDescription: 'A blue smartphone', minPriceMinor: 4199900,
    thumbnail: { url: 'https://example.test/phone.jpg' } },
  { id: 'shirt', legacyId: 11, name: 'Cotton T-Shirt', categoryId: 'fashion',
    shortDescription: 'Soft cotton', minPriceMinor: 69900 },
];

test('question length and type are bounded', () => {
  assert.equal(validateQuestion('  blue   phone  '), 'blue phone');
  assert.equal(validateQuestion('hi'), null);
  assert.equal(validateQuestion('x'.repeat(501)), null);
  assert.equal(validateQuestion({ question: 'phone' }), null);
});

test('minor currency units become rupees before reaching the model', () => {
  assert.match(formatPrice(4199900), /41,999/);
  assert.equal(formatPrice('invalid'), 'Price unavailable');
});

test('catalog matching excludes unrelated products', () => {
  assert.deepEqual(selectProducts('blue phone', products).map((item) => item.id), ['phone']);
  assert.deepEqual(selectProducts('unlisted bicycle', products), []);
});

test('assistant links and prices come only from matched catalog data', () => {
  const answer = parseGroundedAnswer(JSON.stringify({ answer: 'The phone is available.',
    productIds: ['phone', 'fake', 'phone'] }), products.slice(0, 1));
  assert.equal(answer.products.length, 1);
  assert.equal(answer.products[0].priceMinor, 4199900);
  assert.equal(answer.products[0].legacyId, 10);
  assert.throws(() => parseGroundedAnswer('{not-json}', products));
});

test('an invented or misread numeric amount is removed from the answer', () => {
  const answer = parseGroundedAnswer(JSON.stringify({
    answer: 'The phone costs 4,199,900.', productIds: ['phone'],
  }), products);
  assert.doesNotMatch(answer.answer, /4,199,900/);
  assert.equal(answer.products[0].priceMinor, 4199900);
});
