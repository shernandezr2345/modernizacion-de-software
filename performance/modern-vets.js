import http from 'k6/http';
import { check, sleep } from 'k6';

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
const token = __ENV.JWT_TOKEN;

export default function () {
	const response = http.get(`${baseUrl}/api/v1/vets`, {
		headers: {
			Authorization: `Bearer ${token}`,
			Accept: 'application/json',
		},
	});

	check(response, {
		'status is 200': (r) => r.status === 200,
		'response is JSON': (r) =>
			String(r.headers['Content-Type']).includes('application/json'),
		'body is array': (r) => {
			try {
				const body = JSON.parse(r.body);
				return Array.isArray(body);
			} catch {
				return false;
			}
		},
	});

	sleep(0.1);
}
