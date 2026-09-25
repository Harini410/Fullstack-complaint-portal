const BASE_URL = process.env.REACT_APP_API_URL 
  || (typeof window !== 'undefined' && window.location.port === '3000' && window.location.hostname === 'localhost' ? 'http://localhost:8080/api' : '/api');

// Helper to attach JWT Bearer token if user is logged in
const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
};

// --- Complaint Endpoints ---
export const fetchComplaints = async () => {
  const res = await fetch(`${BASE_URL}/complaints`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    throw new Error('Failed to fetch complaints');
  }
  return res.json();
};

export const submitComplaint = async (complaintData) => {
  const res = await fetch(`${BASE_URL}/complaints`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(complaintData),
  });
  if (!res.ok) {
    throw new Error('Failed to submit complaint');
  }
  return res.json();
};

export const deleteComplaintApi = async (id, reason = '') => {
  const query = reason ? `?reason=${encodeURIComponent(reason)}` : '';
  const res = await fetch(`${BASE_URL}/complaints/${id}${query}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    if (res.status === 401) {
      throw new Error('Authentication required: Please log in to delete complaints.');
    }
    if (res.status === 403) {
      throw new Error('Access denied: You can only delete your own complaints or need Admin permissions.');
    }
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message || 'Failed to delete complaint');
  }
};

export const fetchDeletedComplaintsApi = async () => {
  const res = await fetch(`${BASE_URL}/admin/deleted-complaints`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) return [];
  return res.json();
};

export const restoreComplaintApi = async (id) => {
  const res = await fetch(`${BASE_URL}/admin/complaints/${id}/restore`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message || 'Failed to restore complaint');
  }
  return res.json();
};

export const updateComplaintStatusApi = async (id, status, remarks = '') => {
  const res = await fetch(`${BASE_URL}/complaints/${id}/status`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
    body: JSON.stringify({ status, remarks }),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message || 'Failed to update complaint status');
  }
  return res.json();
};

export const assignComplaintApi = async (id, agentId) => {
  const res = await fetch(`${BASE_URL}/complaints/${id}/assign`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
    body: JSON.stringify({ agentId }),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message || 'Failed to assign agent');
  }
  return res.json();
};

export const fetchSupportAgentsApi = async () => {
  const res = await fetch(`${BASE_URL}/admin/support-agents`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) return [];
  return res.json();
};

// --- Authentication Endpoints ---
export const loginApi = async (username, password) => {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message || 'Login failed');
  }
  return res.json();
};

export const registerApi = async (userData) => {
  const res = await fetch(`${BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(userData),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message || 'Registration failed');
  }
  return res.json();
};

// --- Categories & Stats ---
export const fetchCategoriesApi = async () => {
  const res = await fetch(`${BASE_URL}/categories`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) return [];
  return res.json();
};

export const fetchStatisticsApi = async () => {
  const res = await fetch(`${BASE_URL}/admin/statistics`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) return null;
  return res.json();
};

// --- Comments & Notes ---
export const fetchCommentsApi = async (complaintId) => {
  const res = await fetch(`${BASE_URL}/complaints/${complaintId}/comments`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) return [];
  return res.json();
};

export const addCommentApi = async (complaintId, content) => {
  const res = await fetch(`${BASE_URL}/complaints/${complaintId}/comments`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify({ content }),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message || 'Failed to add comment');
  }
  return res.json();
};
