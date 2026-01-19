import http from 'k6/http';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  scenarios: {
    race_condition: {
      executor: 'per-vu-iterations', 
      vus: 100,
      iterations: 1,
      maxDuration: '1m',
    },
  },
};

export default function () {
  const url = 'http://localhost:8081/api/orders';
  
  const params = {
    headers: { 'Content-Type': 'application/json', 'X-User-Id': String(randomIntBetween(1, 1000)) },
  };

  const payload = JSON.stringify({
    items: [
      {
        productId: 7, 
        productPrice: 10000,
        quantity: 1,    
      },
    ],
  });

  http.post(url, payload, params);
}