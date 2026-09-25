import React, { useEffect, useState } from "react";
import {
  fetchComplaints,
  submitComplaint,
  deleteComplaintApi,
  fetchDeletedComplaintsApi,
  restoreComplaintApi,
  updateComplaintStatusApi,
  assignComplaintApi,
  fetchSupportAgentsApi,
  loginApi,
  fetchCategoriesApi,
  fetchStatisticsApi,
  fetchCommentsApi,
  addCommentApi
} from "./api";

function App() {
  const [complaints, setComplaints] = useState([]);
  const [deletedComplaints, setDeletedComplaints] = useState([]);
  const [adminViewTab, setAdminViewTab] = useState("ACTIVE");
  const [newComplaint, setNewComplaint] = useState("");
  const [category, setCategory] = useState("General");
  const [priority, setPriority] = useState("MEDIUM");
  const [categories, setCategories] = useState([]);

  // Auth, Roles & Filter state
  const [user, setUser] = useState(null);
  const [supportAgents, setSupportAgents] = useState([]);
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [loginUsername, setLoginUsername] = useState("admin");
  const [loginPassword, setLoginPassword] = useState("admin123");
  const [stats, setStats] = useState(null);
  const [notification, setNotification] = useState("");

  // Notes & Investigation state
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [notes, setNotes] = useState([]);
  const [newNote, setNewNote] = useState("");

  const showNotification = (msg) => {
    setNotification(msg);
    setTimeout(() => setNotification(""), 4000);
  };

  const loadComplaints = () => {
    fetchComplaints()
      .then((data) => {
        if (Array.isArray(data)) {
          setComplaints(data);
        } else if (data && Array.isArray(data.content)) {
          setComplaints(data.content);
        }
      })
      .catch((err) => {
        console.error("Error fetching complaints:", err);
        showNotification("Failed to load complaints");
      });
  };

  useEffect(() => {
    loadComplaints();

    fetchCategoriesApi()
      .then((cats) => {
        if (Array.isArray(cats) && cats.length > 0) {
          setCategories(cats);
        }
      })
      .catch(() => {});

    // Check stored user session
    const storedUser = localStorage.getItem("username");
    const storedRole = localStorage.getItem("role");
    const storedId = localStorage.getItem("userId");
    if (storedUser && storedRole) {
      setUser({ userId: storedId ? Number(storedId) : null, username: storedUser, role: storedRole });
      if (storedRole === "ROLE_ADMIN") {
        fetchStatisticsApi().then(setStats).catch(() => {});
        fetchSupportAgentsApi().then(setSupportAgents).catch(() => {});
        fetchDeletedComplaintsApi().then(setDeletedComplaints).catch(() => {});
      }
    }
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!user) {
      setShowLoginModal(true);
      showNotification("Please log in to submit a complaint");
      return;
    }
    if (!newComplaint.trim()) return;

    const payload = {
      title: newComplaint.length > 40 ? newComplaint.substring(0, 37) + "..." : newComplaint,
      description: newComplaint,
      category: category,
      priority: priority,
      status: "OPEN"
    };

    submitComplaint(payload)
      .then((data) => {
        setComplaints((prev) => [data, ...prev]);
        setNewComplaint("");
        showNotification("Complaint submitted successfully!");
        if (user && user.role === "ROLE_ADMIN") {
          fetchStatisticsApi().then(setStats).catch(() => {});
        }
      })
      .catch((err) => {
        console.error("Error submitting complaint:", err);
        showNotification("Error submitting complaint");
      });
  };

  const handleDelete = (id) => {
    if (!user) {
      setShowLoginModal(true);
      showNotification("Authentication required: Please log in to delete complaints.");
      return;
    }
    const reason = prompt("Enter deletion reason or remarks (will be tracked by Admin):", "Withdrawn or resolved");
    deleteComplaintApi(id, reason || "Deleted by " + user.username)
      .then(() => {
        setComplaints((prev) => prev.filter((complaint) => complaint.id !== id));
        showNotification(`Complaint #${id} deleted (audit trail logged for Admin).`);
        if (user && user.role === "ROLE_ADMIN") {
          fetchStatisticsApi().then(setStats).catch(() => {});
          fetchDeletedComplaintsApi().then(setDeletedComplaints).catch(() => {});
        }
      })
      .catch((err) => {
        console.error("Error deleting complaint:", err);
        showNotification(err.message || "Error deleting complaint");
      });
  };

  const handleRestore = (id) => {
    restoreComplaintApi(id)
      .then((restored) => {
        setDeletedComplaints((prev) => prev.filter((c) => c.id !== id));
        setComplaints((prev) => [restored, ...prev]);
        showNotification(`Complaint #${id} restored back to active queue!`);
        if (user && user.role === "ROLE_ADMIN") {
          fetchStatisticsApi().then(setStats).catch(() => {});
        }
      })
      .catch((err) => {
        showNotification(err.message || "Failed to restore complaint");
      });
  };

  const handleStatusChange = (id, newStatus) => {
    updateComplaintStatusApi(id, newStatus, `Updated to ${newStatus} by ${user?.username || 'admin'}`)
      .then((updated) => {
        setComplaints((prev) =>
          prev.map((c) => (c.id === id ? { ...c, status: updated.status } : c))
        );
        showNotification(`Complaint #${id} marked as ${newStatus}!`);
        if (user && user.role === "ROLE_ADMIN") {
          fetchStatisticsApi().then(setStats).catch(() => {});
        }
      })
      .catch((err) => {
        console.error("Error updating complaint status:", err);
        showNotification(err.message || "Failed to update status");
      });
  };

  const handleAssignAgent = (complaintId, agentId) => {
    assignComplaintApi(complaintId, agentId)
      .then((updated) => {
        setComplaints((prev) =>
          prev.map((c) =>
            c.id === complaintId
              ? {
                  ...c,
                  assignedToId: updated.assignedToId,
                  assignedToName: updated.assignedToName,
                  status: updated.status,
                }
              : c
          )
        );
        showNotification(
          `Complaint #${complaintId} assigned to ${updated.assignedToName || 'agent'}!`
        );
        if (user && user.role === "ROLE_ADMIN") {
          fetchStatisticsApi().then(setStats).catch(() => {});
        }
      })
      .catch((err) => {
        console.error("Error assigning complaint:", err);
        showNotification(err.message || "Failed to assign complaint");
      });
  };

  const handleClaimTicket = (complaintId) => {
    if (!user || (user.role !== "ROLE_SUPPORT_AGENT" && user.role !== "ROLE_ADMIN")) return;
    const targetAgentId = user.userId || 2; // fallback to agent ID 2 if not yet stored
    assignComplaintApi(complaintId, targetAgentId)
      .then((updated) => {
        setComplaints((prev) =>
          prev.map((c) =>
            c.id === complaintId
              ? {
                  ...c,
                  assignedToId: updated.assignedToId,
                  assignedToName: updated.assignedToName || user.username,
                  status: updated.status,
                }
              : c
          )
        );
        showNotification(`Ticket #${complaintId} claimed successfully!`);
      })
      .catch((err) => {
        showNotification(err.message || "Failed to claim ticket");
      });
  };

  const handleOpenNotes = (c) => {
    setSelectedComplaint(c);
    fetchCommentsApi(c.id).then(setNotes).catch(() => setNotes([]));
  };

  const handleAddNote = (e) => {
    e.preventDefault();
    if (!newNote.trim() || !selectedComplaint) return;
    addCommentApi(selectedComplaint.id, newNote)
      .then((saved) => {
        setNotes((prev) => [...prev, saved]);
        setNewNote("");
        showNotification("Investigation note logged!");
      })
      .catch((err) => {
        showNotification(err.message || "Failed to add note");
      });
  };

  const handleLogin = (e) => {
    e.preventDefault();
    loginApi(loginUsername, loginPassword)
      .then((res) => {
        localStorage.setItem("token", res.token);
        localStorage.setItem("username", res.username);
        localStorage.setItem("role", res.role);
        localStorage.setItem("userId", res.userId);
        setUser({ userId: res.userId, username: res.username, role: res.role });
        setShowLoginModal(false);
        showNotification(`Logged in as ${res.username} (${res.role})`);
        loadComplaints();
        if (res.role === "ROLE_ADMIN") {
          fetchStatisticsApi().then(setStats).catch(() => {});
          fetchSupportAgentsApi().then(setSupportAgents).catch(() => {});
          fetchDeletedComplaintsApi().then(setDeletedComplaints).catch(() => {});
        }
      })
      .catch((err) => {
        alert("Login failed: " + err.message);
      });
  };

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    localStorage.removeItem("userId");
    setUser(null);
    setStats(null);
    setSupportAgents([]);
    setDeletedComplaints([]);
    setAdminViewTab("ACTIVE");
    setActiveFilter("ALL");
    showNotification("Logged out");
    loadComplaints();
  };

  const getStatusBadgeColor = (status) => {
    switch (status) {
      case "OPEN": return "#28a745";
      case "ASSIGNED": return "#17a2b8";
      case "IN_PROGRESS": return "#ffc107";
      case "RESOLVED": return "#007bff";
      case "CLOSED": return "#6c757d";
      default: return "#007bff";
    }
  };

  const getPriorityBadgeColor = (p) => {
    switch (p) {
      case "CRITICAL": return "#dc3545";
      case "HIGH": return "#fd7e14";
      case "MEDIUM": return "#ffc107";
      case "LOW": return "#6c757d";
      default: return "#6c757d";
    }
  };

  const displayedComplaints = complaints.filter((c) => {
    if (activeFilter === "ASSIGNED_TO_ME") {
      return (
        (user && user.userId && c.assignedToId === user.userId) ||
        c.assignedToName?.toLowerCase().includes("sarah") ||
        c.assignedToName?.toLowerCase().includes("support")
      );
    }
    if (activeFilter === "UNASSIGNED") {
      return !c.assignedToName;
    }
    if (activeFilter === "PENDING") {
      return c.status === "OPEN" || c.status === "ASSIGNED" || c.status === "IN_PROGRESS";
    }
    return true;
  });

  return (
    <div style={{ margin: "2rem auto", maxWidth: "960px", fontFamily: "Segoe UI, Roboto, Helvetica, Arial, sans-serif" }}>
      {/* Top Bar with Auth Details */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem", borderBottom: "1px solid #e0e0e0", paddingBottom: "1rem" }}>
        <div>
          <h1 style={{ margin: 0, color: "#1a365d" }}>Smart Complaint Portal</h1>
          <small style={{ color: "#718096" }}>Production-Grade Incident Management System</small>
        </div>
        <div>
          {user ? (
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <span style={{ fontSize: "14px", color: "#2d3748" }}>
                Logged in as: <strong>{user.username}</strong> ({user.role})
              </span>
              <button
                onClick={handleLogout}
                style={{ padding: "6px 12px", backgroundColor: "#e2e8f0", border: "none", borderRadius: "4px", cursor: "pointer" }}
              >
                Logout
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowLoginModal(true)}
              style={{ padding: "8px 16px", backgroundColor: "#3182ce", color: "white", border: "none", borderRadius: "5px", cursor: "pointer", fontWeight: "bold" }}
            >
              Portal Login
            </button>
          )}
        </div>
      </div>

      {/* Support Agent Workstation Banner (Visible when Support Agent is logged in) */}
      {user && user.role === "ROLE_SUPPORT_AGENT" && (
        <div style={{ marginBottom: "1.5rem", padding: "14px 20px", backgroundColor: "#ebf8ff", borderRadius: "8px", border: "1px solid #bee3f8", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "10px" }}>
          <div>
            <div style={{ fontWeight: "bold", color: "#2b6cb0", fontSize: "16px" }}>
              🛠️ Support Agent Workstation
            </div>
            <small style={{ color: "#4a5568" }}>
              Field Operations & Incident Resolution Desk — Logged in as: <strong>{user.username}</strong>
            </small>
          </div>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
            <button
              onClick={() => setActiveFilter("ALL")}
              style={{
                padding: "6px 14px",
                borderRadius: "20px",
                border: "1px solid #3182ce",
                backgroundColor: activeFilter === "ALL" ? "#3182ce" : "white",
                color: activeFilter === "ALL" ? "white" : "#3182ce",
                cursor: "pointer",
                fontSize: "12px",
                fontWeight: "600"
              }}
            >
              All Tickets ({complaints.length})
            </button>
            <button
              onClick={() => setActiveFilter("ASSIGNED_TO_ME")}
              style={{
                padding: "6px 14px",
                borderRadius: "20px",
                border: "1px solid #3182ce",
                backgroundColor: activeFilter === "ASSIGNED_TO_ME" ? "#3182ce" : "white",
                color: activeFilter === "ASSIGNED_TO_ME" ? "white" : "#3182ce",
                cursor: "pointer",
                fontSize: "12px",
                fontWeight: "600"
              }}
            >
              🎯 Assigned to Me ({complaints.filter(c => (user && user.userId && c.assignedToId === user.userId) || c.assignedToName?.toLowerCase().includes("sarah") || c.assignedToName?.toLowerCase().includes("support")).length})
            </button>
            <button
              onClick={() => setActiveFilter("UNASSIGNED")}
              style={{
                padding: "6px 14px",
                borderRadius: "20px",
                border: "1px solid #805ad5",
                backgroundColor: activeFilter === "UNASSIGNED" ? "#805ad5" : "white",
                color: activeFilter === "UNASSIGNED" ? "white" : "#805ad5",
                cursor: "pointer",
                fontSize: "12px",
                fontWeight: "600"
              }}
            >
              📥 Unassigned ({complaints.filter(c => !c.assignedToName).length})
            </button>
            <button
              onClick={() => setActiveFilter("PENDING")}
              style={{
                padding: "6px 14px",
                borderRadius: "20px",
                border: "1px solid #dd6b20",
                backgroundColor: activeFilter === "PENDING" ? "#dd6b20" : "white",
                color: activeFilter === "PENDING" ? "white" : "#dd6b20",
                cursor: "pointer",
                fontSize: "12px",
                fontWeight: "600"
              }}
            >
              ⏳ Pending Action ({complaints.filter(c => c.status === "OPEN" || c.status === "ASSIGNED" || c.status === "IN_PROGRESS").length})
            </button>
          </div>
        </div>
      )}

      {/* Toast Notification */}
      {notification && (
        <div style={{ padding: "10px 16px", marginBottom: "1rem", backgroundColor: "#ebf8ff", color: "#2b6cb0", borderRadius: "6px", border: "1px solid #bee3f8" }}>
          {notification}
        </div>
      )}

      {/* Admin Metrics Widget (Visible when Admin is logged in) */}
      {user && user.role === "ROLE_ADMIN" && stats && (
        <div style={{ marginBottom: "2rem", padding: "1rem", backgroundColor: "#f7fafc", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
          <h3 style={{ margin: "0 0 10px 0", color: "#2d3748" }}>Admin Dashboard Statistics</h3>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "15px" }}>
            <div style={{ background: "white", padding: "10px 15px", borderRadius: "6px", border: "1px solid #edf2f7", minWidth: "100px", textAlign: "center" }}>
              <div style={{ fontSize: "20px", fontWeight: "bold", color: "#2b6cb0" }}>{stats.totalComplaints}</div>
              <div style={{ fontSize: "12px", color: "#718096" }}>Active Total</div>
            </div>
            <div style={{ background: "white", padding: "10px 15px", borderRadius: "6px", border: "1px solid #edf2f7", minWidth: "100px", textAlign: "center" }}>
              <div style={{ fontSize: "20px", fontWeight: "bold", color: "#38a169" }}>{stats.openComplaints}</div>
              <div style={{ fontSize: "12px", color: "#718096" }}>Open</div>
            </div>
            <div style={{ background: "white", padding: "10px 15px", borderRadius: "6px", border: "1px solid #edf2f7", minWidth: "100px", textAlign: "center" }}>
              <div style={{ fontSize: "20px", fontWeight: "bold", color: "#dd6b20" }}>{stats.inProgressComplaints}</div>
              <div style={{ fontSize: "12px", color: "#718096" }}>In Progress</div>
            </div>
            <div style={{ background: "white", padding: "10px 15px", borderRadius: "6px", border: "1px solid #edf2f7", minWidth: "100px", textAlign: "center" }}>
              <div style={{ fontSize: "20px", fontWeight: "bold", color: "#3182ce" }}>{stats.resolvedComplaints}</div>
              <div style={{ fontSize: "12px", color: "#718096" }}>Resolved</div>
            </div>
            <div style={{ background: "white", padding: "10px 15px", borderRadius: "6px", border: "1px solid #edf2f7", minWidth: "100px", textAlign: "center" }}>
              <div style={{ fontSize: "20px", fontWeight: "bold", color: "#e53e3e" }}>{stats.criticalPriority}</div>
              <div style={{ fontSize: "12px", color: "#718096" }}>Critical Priority</div>
            </div>
            {stats.deletedComplaints !== undefined && (
              <div style={{ background: "white", padding: "10px 15px", borderRadius: "6px", border: "1px solid #edf2f7", minWidth: "100px", textAlign: "center" }}>
                <div style={{ fontSize: "20px", fontWeight: "bold", color: "#718096" }}>{stats.deletedComplaints}</div>
                <div style={{ fontSize: "12px", color: "#e53e3e" }}>Deleted (Tracked)</div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Submission Form (Only visible for authenticated citizens / users and admins) */}
      {user && (user.role === "ROLE_USER" || user.role === "ROLE_ADMIN") && (
        <div style={{ background: "#ffffff", padding: "20px", borderRadius: "8px", border: "1px solid #e2e8f0", marginBottom: "2rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
            <h2 style={{ margin: 0, color: "#2d3748", fontSize: "18px" }}>Log New Complaint</h2>
            <span style={{ fontSize: "12px", color: "#4a5568", backgroundColor: "#edf2f7", padding: "3px 8px", borderRadius: "4px" }}>
              Submitting as: <strong>{user.username}</strong> ({user.role})
            </span>
          </div>
          <form onSubmit={handleSubmit} style={{ display: "flex", flexWrap: "wrap", gap: "10px", alignItems: "center" }}>
            <input
              type="text"
              value={newComplaint}
              onChange={(e) => setNewComplaint(e.target.value)}
              placeholder="Enter complaint description..."
              required
              style={{
                padding: "10px 12px",
                flex: "1 1 300px",
                borderRadius: "5px",
                border: "1px solid #cbd5e0",
                fontSize: "14px",
              }}
            />

            <select
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              style={{ padding: "10px", borderRadius: "5px", border: "1px solid #cbd5e0", fontSize: "14px" }}
            >
              {categories.length > 0 ? (
                categories.map((cat) => (
                  <option key={cat.id} value={cat.name}>{cat.name}</option>
                ))
              ) : (
                <>
                  <option value="General">General</option>
                  <option value="Electricity">Electricity</option>
                  <option value="Water">Water</option>
                  <option value="Sanitation">Sanitation</option>
                  <option value="Roads & Infrastructure">Roads & Infrastructure</option>
                </>
              )}
            </select>

            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
              style={{ padding: "10px", borderRadius: "5px", border: "1px solid #cbd5e0", fontSize: "14px" }}
            >
              <option value="LOW">Low Priority</option>
              <option value="MEDIUM">Medium Priority</option>
              <option value="HIGH">High Priority</option>
              <option value="CRITICAL">Critical Priority</option>
            </select>

            <button
              type="submit"
              style={{
                padding: "10px 20px",
                backgroundColor: "#3182ce",
                color: "white",
                border: "none",
                borderRadius: "5px",
                cursor: "pointer",
                fontWeight: "600",
              }}
            >
              Submit Complaint
            </button>
          </form>
        </div>
      )}

      {/* Admin Tab Switcher: Active Complaints vs Deleted Complaints Audit Log */}
      {user && user.role === "ROLE_ADMIN" && (
        <div style={{ display: "flex", gap: "10px", marginBottom: "1.5rem" }}>
          <button
            onClick={() => setAdminViewTab("ACTIVE")}
            style={{
              padding: "10px 18px",
              borderRadius: "6px",
              border: adminViewTab === "ACTIVE" ? "2px solid #3182ce" : "1px solid #cbd5e0",
              backgroundColor: adminViewTab === "ACTIVE" ? "#3182ce" : "#ffffff",
              color: adminViewTab === "ACTIVE" ? "#ffffff" : "#2b6cb0",
              fontWeight: "bold",
              cursor: "pointer",
              fontSize: "14px",
              boxShadow: adminViewTab === "ACTIVE" ? "0 2px 4px rgba(49,130,206,0.3)" : "none"
            }}
          >
            📋 Active Complaints ({complaints.length})
          </button>
          <button
            onClick={() => {
              setAdminViewTab("DELETED_AUDIT");
              fetchDeletedComplaintsApi().then(setDeletedComplaints).catch(() => {});
            }}
            style={{
              padding: "10px 18px",
              borderRadius: "6px",
              border: adminViewTab === "DELETED_AUDIT" ? "2px solid #e53e3e" : "1px solid #cbd5e0",
              backgroundColor: adminViewTab === "DELETED_AUDIT" ? "#e53e3e" : "#ffffff",
              color: adminViewTab === "DELETED_AUDIT" ? "#ffffff" : "#c53030",
              fontWeight: "bold",
              cursor: "pointer",
              fontSize: "14px",
              boxShadow: adminViewTab === "DELETED_AUDIT" ? "0 2px 4px rgba(229,62,62,0.3)" : "none"
            }}
          >
            🗑️ Deleted Complaints Audit Log ({deletedComplaints.length})
          </button>
        </div>
      )}

      {user && user.role === "ROLE_ADMIN" && adminViewTab === "DELETED_AUDIT" ? (
        <div style={{ backgroundColor: "#fff", padding: "20px", borderRadius: "8px", border: "1px solid #fed7d7", boxShadow: "0 2px 4px rgba(0,0,0,0.05)", marginBottom: "2rem" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
            <div>
              <h2 style={{ margin: 0, color: "#c53030", fontSize: "18px" }}>
                🛡️ Deleted Complaints Audit Log ({deletedComplaints.length})
              </h2>
              <small style={{ color: "#718096" }}>
                Compliance Audit Trail: Full visibility into all deleted tickets, including who deleted them, reasons, and timestamps.
              </small>
            </div>
            <button
              onClick={() => fetchDeletedComplaintsApi().then(setDeletedComplaints).catch(() => {})}
              style={{ padding: "6px 12px", backgroundColor: "#edf2f7", border: "1px solid #cbd5e0", borderRadius: "4px", cursor: "pointer", fontSize: "12px" }}
            >
              🔄 Refresh Audit Log
            </button>
          </div>

          {deletedComplaints.length === 0 ? (
            <p style={{ color: "#718096", fontStyle: "italic" }}>No deleted complaints found in audit history.</p>
          ) : (
            <table
              style={{
                borderCollapse: "collapse",
                width: "100%",
                backgroundColor: "#fff",
                borderRadius: "6px",
                overflow: "hidden",
                fontSize: "13px"
              }}
            >
              <thead>
                <tr style={{ backgroundColor: "#e53e3e", color: "white", textAlign: "left" }}>
                  <th style={{ border: "1px solid #feb2b2", padding: "10px" }}>ID</th>
                  <th style={{ border: "1px solid #feb2b2", padding: "10px" }}>Category</th>
                  <th style={{ border: "1px solid #feb2b2", padding: "10px" }}>Description</th>
                  <th style={{ border: "1px solid #feb2b2", padding: "10px" }}>Submitted By</th>
                  <th style={{ border: "1px solid #feb2b2", padding: "10px" }}>Deleted By</th>
                  <th style={{ border: "1px solid #feb2b2", padding: "10px" }}>Deletion Reason</th>
                  <th style={{ border: "1px solid #feb2b2", padding: "10px" }}>Deleted At</th>
                  <th style={{ border: "1px solid #feb2b2", padding: "10px" }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {deletedComplaints.map((c) => (
                  <tr key={c.id} style={{ borderBottom: "1px solid #fed7d7", backgroundColor: "#fff5f5" }}>
                    <td style={{ border: "1px solid #fed7d7", padding: "10px", fontWeight: "bold" }}>#{c.id}</td>
                    <td style={{ border: "1px solid #fed7d7", padding: "10px" }}>
                      <span style={{ background: "#edf2f7", padding: "3px 6px", borderRadius: "4px", fontSize: "11px" }}>
                        {c.category}
                      </span>
                    </td>
                    <td style={{ border: "1px solid #fed7d7", padding: "10px" }}>{c.description}</td>
                    <td style={{ border: "1px solid #fed7d7", padding: "10px" }}>
                      👤 {c.createdByName || "Anonymous"}
                    </td>
                    <td style={{ border: "1px solid #fed7d7", padding: "10px" }}>
                      <span style={{ color: "#c53030", fontWeight: "bold" }}>👤 {c.deletedByName || "User"}</span>
                      {c.deletedByRole && <span style={{ fontSize: "11px", color: "#718096", display: "block" }}>({c.deletedByRole})</span>}
                    </td>
                    <td style={{ border: "1px solid #fed7d7", padding: "10px", color: "#4a5568" }}>
                      {c.deleteReason || "Withdrawn / deleted"}
                    </td>
                    <td style={{ border: "1px solid #fed7d7", padding: "10px", color: "#718096", fontSize: "11px" }}>
                      {c.deletedAt ? new Date(c.deletedAt).toLocaleString() : "N/A"}
                    </td>
                    <td style={{ border: "1px solid #fed7d7", padding: "10px" }}>
                      <button
                        onClick={() => handleRestore(c.id)}
                        style={{
                          padding: "6px 12px",
                          backgroundColor: "#3182ce",
                          color: "white",
                          border: "none",
                          borderRadius: "4px",
                          cursor: "pointer",
                          fontSize: "12px",
                          fontWeight: "bold",
                        }}
                        title="Restore this complaint back to active list"
                      >
                        🔄 Restore
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      ) : (
        /* Active Complaints Section */
        <div>
          <h2>Complaints List ({displayedComplaints.length})</h2>

          {displayedComplaints.length === 0 ? (
            <p style={{ color: "#718096" }}>No complaints found matching the current filter.</p>
          ) : (
            <table
              style={{
                borderCollapse: "collapse",
                width: "100%",
                backgroundColor: "#fff",
                borderRadius: "8px",
                overflow: "hidden",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
              }}
            >
              <thead>
                <tr style={{ backgroundColor: "#3182ce", color: "white", textAlign: "left" }}>
                  <th style={{ border: "1px solid #e2e8f0", padding: "12px" }}>ID</th>
                  <th style={{ border: "1px solid #e2e8f0", padding: "12px" }}>Category</th>
                  <th style={{ border: "1px solid #e2e8f0", padding: "12px" }}>Description</th>
                  <th style={{ border: "1px solid #e2e8f0", padding: "12px" }}>Priority</th>
                  <th style={{ border: "1px solid #e2e8f0", padding: "12px" }}>Assigned To</th>
                  <th style={{ border: "1px solid #e2e8f0", padding: "12px" }}>Status</th>
                  {user && (
                    <th style={{ border: "1px solid #e2e8f0", padding: "12px" }}>Action</th>
                  )}
                </tr>
              </thead>

          <tbody>
            {displayedComplaints.map((c) => (
              <tr key={c.id} style={{ borderBottom: "1px solid #edf2f7" }}>
                <td style={{ border: "1px solid #edf2f7", padding: "10px" }}>#{c.id}</td>
                <td style={{ border: "1px solid #edf2f7", padding: "10px" }}>
                  <span style={{ background: "#edf2f7", padding: "4px 8px", borderRadius: "4px", fontSize: "12px" }}>
                    {c.category}
                  </span>
                </td>
                <td style={{ border: "1px solid #edf2f7", padding: "10px" }}>{c.description}</td>
                <td style={{ border: "1px solid #edf2f7", padding: "10px" }}>
                  <span style={{
                    color: getPriorityBadgeColor(c.priority),
                    fontWeight: "bold",
                    fontSize: "12px"
                  }}>
                    {c.priority || "MEDIUM"}
                  </span>
                </td>
                <td style={{ border: "1px solid #edf2f7", padding: "10px" }}>
                  {user && user.role === "ROLE_ADMIN" ? (
                    <select
                      value={c.assignedToId || ""}
                      onChange={(e) => {
                        if (e.target.value) {
                          handleAssignAgent(c.id, Number(e.target.value));
                        }
                      }}
                      style={{
                        padding: "4px 8px",
                        borderRadius: "4px",
                        border: "1px solid #cbd5e0",
                        fontSize: "12px",
                        backgroundColor: c.assignedToName ? "#ebf8ff" : "#fff",
                        color: c.assignedToName ? "#2b6cb0" : "#718096",
                        fontWeight: c.assignedToName ? "600" : "normal",
                        cursor: "pointer"
                      }}
                      title="Assign support agent"
                    >
                      <option value="">{c.assignedToName ? `👤 ${c.assignedToName}` : "-- Assign Agent --"}</option>
                      {supportAgents.map((agent) => (
                        <option key={agent.id} value={agent.id}>
                          👤 {agent.fullName || agent.username}
                        </option>
                      ))}
                    </select>
                  ) : user && user.role === "ROLE_SUPPORT_AGENT" && !c.assignedToName ? (
                    <button
                      onClick={() => handleClaimTicket(c.id)}
                      style={{
                        padding: "5px 10px",
                        backgroundColor: "#319795",
                        color: "white",
                        border: "none",
                        borderRadius: "4px",
                        fontSize: "12px",
                        fontWeight: "600",
                        cursor: "pointer"
                      }}
                      title="Assign this ticket to yourself"
                    >
                      ✋ Claim Ticket
                    </button>
                  ) : (
                    c.assignedToName ? (
                      <span style={{
                        backgroundColor: (user && user.role === "ROLE_SUPPORT_AGENT" && (c.assignedToId === user.userId || c.assignedToName?.toLowerCase().includes("sarah"))) ? "#c6f6d5" : "#ebf8ff",
                        color: (user && user.role === "ROLE_SUPPORT_AGENT" && (c.assignedToId === user.userId || c.assignedToName?.toLowerCase().includes("sarah"))) ? "#22543d" : "#2b6cb0",
                        padding: "3px 8px",
                        borderRadius: "12px",
                        fontSize: "12px",
                        fontWeight: "600",
                        display: "inline-block"
                      }}>
                        {(user && user.role === "ROLE_SUPPORT_AGENT" && (c.assignedToId === user.userId || c.assignedToName?.toLowerCase().includes("sarah")))
                          ? `🎯 Assigned to You (${c.assignedToName})`
                          : `👤 ${c.assignedToName}`}
                      </span>
                    ) : (
                      <span style={{ color: "#a0aec0", fontSize: "12px", fontStyle: "italic" }}>
                        Unassigned
                      </span>
                    )
                  )}
                </td>
                <td style={{ border: "1px solid #edf2f7", padding: "10px" }}>
                  {user && (user.role === "ROLE_ADMIN" || user.role === "ROLE_SUPPORT_AGENT") ? (
                    <select
                      value={c.status}
                      onChange={(e) => handleStatusChange(c.id, e.target.value)}
                      style={{
                        padding: "4px 8px",
                        borderRadius: "12px",
                        fontSize: "12px",
                        fontWeight: "bold",
                        backgroundColor: getStatusBadgeColor(c.status),
                        color: "white",
                        border: "none",
                        cursor: "pointer",
                      }}
                    >
                      <option value="OPEN" style={{ color: "black", backgroundColor: "white" }}>OPEN</option>
                      <option value="ASSIGNED" style={{ color: "black", backgroundColor: "white" }}>ASSIGNED</option>
                      <option value="IN_PROGRESS" style={{ color: "black", backgroundColor: "white" }}>IN_PROGRESS</option>
                      <option value="RESOLVED" style={{ color: "black", backgroundColor: "white" }}>RESOLVED</option>
                      <option value="CLOSED" style={{ color: "black", backgroundColor: "white" }}>CLOSED</option>
                    </select>
                  ) : (
                    <span style={{
                      backgroundColor: getStatusBadgeColor(c.status),
                      color: "white",
                      padding: "3px 8px",
                      borderRadius: "12px",
                      fontSize: "12px",
                      fontWeight: "bold"
                    }}>
                      {c.status}
                    </span>
                  )}
                </td>
                {user && (
                  <td style={{ border: "1px solid #edf2f7", padding: "10px" }}>
                    <div style={{ display: "flex", gap: "6px", alignItems: "center" }}>
                      {(user.role === "ROLE_ADMIN" || user.role === "ROLE_SUPPORT_AGENT") && (
                        <>
                          <button
                            onClick={() => handleOpenNotes(c)}
                            style={{
                              padding: "6px 12px",
                              backgroundColor: "#4a5568",
                              color: "white",
                              border: "none",
                              borderRadius: "4px",
                              cursor: "pointer",
                              fontSize: "12px",
                              fontWeight: "500",
                            }}
                            title="View investigation notes"
                          >
                            Notes
                          </button>
                          {c.status !== "RESOLVED" && c.status !== "CLOSED" && (
                            <button
                              onClick={() => handleStatusChange(c.id, "RESOLVED")}
                              style={{
                                padding: "6px 12px",
                                backgroundColor: "#38a169",
                                color: "white",
                                border: "none",
                                borderRadius: "4px",
                                cursor: "pointer",
                                fontSize: "12px",
                                fontWeight: "600",
                              }}
                              title="Mark complaint as Resolved"
                            >
                              Resolve
                            </button>
                          )}
                        </>
                      )}

                      {/* Delete button: Available to Admin, Support Agent, or citizen who submitted the complaint */}
                      {(user.role === "ROLE_ADMIN" ||
                        user.role === "ROLE_SUPPORT_AGENT" ||
                        (user.role === "ROLE_USER" && (c.createdById === user.userId || c.createdByName === user.username))) ? (
                        <button
                          onClick={() => handleDelete(c.id)}
                          style={{
                            padding: "6px 12px",
                            backgroundColor: "#e53e3e",
                            color: "white",
                            border: "none",
                            borderRadius: "4px",
                            cursor: "pointer",
                            fontSize: "12px",
                            fontWeight: "500",
                          }}
                          title={user.role === "ROLE_USER" ? "Delete your complaint (will be recorded in audit log)" : "Delete complaint"}
                        >
                          Delete
                        </button>
                      ) : (
                        user.role === "ROLE_USER" && (
                          <span style={{ color: "#a0aec0", fontSize: "12px", fontStyle: "italic" }}>Read-only</span>
                        )
                      )}
                    </div>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )}

      {/* Login Modal */}
      {showLoginModal && (
        <div style={{
          position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: "rgba(0,0,0,0.5)", display: "flex", justifyContent: "center", alignItems: "center"
        }}>
          <div style={{ background: "white", padding: "24px", borderRadius: "8px", width: "320px" }}>
            <h3 style={{ marginTop: 0 }}>Portal Authentication</h3>
            <form onSubmit={handleLogin}>
              <div style={{ marginBottom: "12px" }}>
                <label style={{ display: "block", fontSize: "12px", marginBottom: "4px" }}>Username</label>
                <input
                  type="text"
                  value={loginUsername}
                  onChange={(e) => setLoginUsername(e.target.value)}
                  style={{ width: "93%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e0" }}
                  required
                />
              </div>
              <div style={{ marginBottom: "16px" }}>
                <label style={{ display: "block", fontSize: "12px", marginBottom: "4px" }}>Password</label>
                <input
                  type="password"
                  value={loginPassword}
                  onChange={(e) => setLoginPassword(e.target.value)}
                  style={{ width: "93%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e0" }}
                  required
                />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
                <button
                  type="button"
                  onClick={() => setShowLoginModal(false)}
                  style={{ padding: "8px 12px", border: "none", background: "#edf2f7", borderRadius: "4px", cursor: "pointer" }}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  style={{ padding: "8px 16px", border: "none", background: "#3182ce", color: "white", borderRadius: "4px", cursor: "pointer", fontWeight: "bold" }}
                >
                  Login
                </button>
              </div>
            </form>
            <div style={{ marginTop: "12px", fontSize: "11px", color: "#718096" }}>
              Default accounts:
              <br />Admin: <code>admin</code> / <code>admin123</code>
              <br />Agents (12+ pool): <code>support_agent</code>, <code>agent_alex</code>, <code>agent_priya</code>, <code>agent_david</code>, etc. / <code>agent123</code>
              <br />User: <code>demo_user</code> / <code>user123</code>
            </div>
          </div>
        </div>
      )}

      {/* Investigation Notes Modal */}
      {selectedComplaint && (
        <div style={{
          position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: "rgba(0,0,0,0.5)", display: "flex", justifyContent: "center", alignItems: "center", zIndex: 1000
        }}>
          <div style={{ background: "white", padding: "24px", borderRadius: "8px", width: "500px", maxHeight: "85vh", overflowY: "auto" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
              <h3 style={{ margin: 0, color: "#2d3748" }}>
                Incident #{selectedComplaint.id}: {selectedComplaint.title}
              </h3>
              <button
                onClick={() => setSelectedComplaint(null)}
                style={{ border: "none", background: "none", fontSize: "18px", cursor: "pointer", color: "#a0aec0" }}
              >
                ✕
              </button>
            </div>

            <div style={{ background: "#f7fafc", padding: "12px", borderRadius: "6px", marginBottom: "16px", fontSize: "13px" }}>
              <div><strong>Description:</strong> {selectedComplaint.description}</div>
              <div style={{ marginTop: "4px" }}><strong>Category:</strong> {selectedComplaint.category} | <strong>Priority:</strong> {selectedComplaint.priority}</div>
              <div style={{ marginTop: "4px" }}><strong>Assigned To:</strong> {selectedComplaint.assignedToName || "Unassigned"}</div>
              <div style={{ marginTop: "4px" }}><strong>Current Status:</strong> <span style={{ fontWeight: "bold", color: getStatusBadgeColor(selectedComplaint.status) }}>{selectedComplaint.status}</span></div>
            </div>

            <h4 style={{ margin: "0 0 10px 0", color: "#4a5568", fontSize: "14px" }}>
              Investigation & Staff Notes ({notes.length})
            </h4>

            {notes.length === 0 ? (
              <p style={{ color: "#a0aec0", fontSize: "13px", fontStyle: "italic", marginBottom: "16px" }}>No investigation notes logged yet.</p>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "10px", marginBottom: "16px" }}>
                {notes.map((n) => (
                  <div key={n.id} style={{ background: "#edf2f7", padding: "10px", borderRadius: "6px", fontSize: "13px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "4px" }}>
                      <strong style={{ color: "#2b6cb0" }}>👤 {n.authorName || "Staff"}</strong>
                      <span style={{ fontSize: "11px", color: "#718096" }}>{n.createdAt ? new Date(n.createdAt).toLocaleString() : ""}</span>
                    </div>
                    <div style={{ color: "#2d3748" }}>{n.content}</div>
                  </div>
                ))}
              </div>
            )}

            {/* Staff note submission form */}
            <form onSubmit={handleAddNote}>
              <textarea
                value={newNote}
                onChange={(e) => setNewNote(e.target.value)}
                placeholder="Log field inspection updates, technician findings, or resolution remarks..."
                rows="3"
                style={{ width: "95%", padding: "8px", borderRadius: "4px", border: "1px solid #cbd5e0", fontSize: "13px", marginBottom: "10px" }}
                required
              />
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "8px" }}>
                <button
                  type="button"
                  onClick={() => setSelectedComplaint(null)}
                  style={{ padding: "8px 14px", border: "none", background: "#edf2f7", borderRadius: "4px", cursor: "pointer", fontSize: "13px" }}
                >
                  Close
                </button>
                <button
                  type="submit"
                  style={{ padding: "8px 16px", border: "none", background: "#3182ce", color: "white", borderRadius: "4px", cursor: "pointer", fontWeight: "bold", fontSize: "13px" }}
                >
                  Add Staff Note
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
