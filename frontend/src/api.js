const BASE_URL = 'http://localhost:8080';

export const fetchComplaints = async () => {
  const res = await fetch(`${BASE_URL}/complaints`);

  if (!res.ok) {
    throw new Error('Failed to fetch complaints');
  }

  return res.json();
};

export const submitComplaint = async (complaint) => {
  const res = await fetch(`${BASE_URL}/complaints`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(complaint)
  });

  if (!res.ok) {
    throw new Error('Failed to submit complaint');
  }

  return res.json();
};
