import http from 'k6/http';
import { check, sleep } from 'k6';

// NOTE: The legacy GET /vets endpoint returns HTML by default (Thymeleaf view).
// When called with Accept: application/json it returns JSON wrapped in {"vetList":[...]}.
// This structural difference is a known threat to validity when comparing with
// the modernized endpoint, which returns a plain JSON array.
// The throughput comparison is still meaningful as both endpoints query the same
// underlying data via VetRepository and the same H2/MySQL/PostgreSQL database.

export const options = {
	stages: [
		{ duration: '30s', target: 20 },
		{ duration: '2m', target: 20 },
		{ duration: '30s', target: 0 },
	],
	thresholds: {
		http_req_failed: ['rate==0'],
		http_req_duration: ['p(95)<500'],
	},
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
	const response = http.get(`${baseUrl}/vets`, {
		headers: {
			Accept: 'application/json',
		},
	});

	check(response, {
		'status is 200': (r) => r.status === 200,
		'response is JSON': (r) =>
			String(r.headers['Content-Type']).includes('application/json'),
		'body has vetList': (r) => {
			try {
				const body = JSON.parse(r.body);
				return Array.isArray(body.vetList);
			} catch {
				return false;
			}
		},
	});

	sleep(0.1);
}
