import http from 'k6/http';
import { check } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
  scenarios: {
    order_scenario: {
      executor: 'shared-iterations',
      
      vus: 5000, 
      
      iterations: 40000, 
      
      maxDuration: '10m', 
    },
  },
};

export default function () {
  const url = 'http://localhost:8081/api/orders';

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': String(randomIntBetween(1, 1000)),
    },
  };

  const payload = JSON.stringify({
    items: [
      {
        productId: randomIntBetween(1, 100), 
        productPrice: 10000,
        quantity: randomIntBetween(1, 3),    
      },
    ],
  });

  const res = http.post(url, payload, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}