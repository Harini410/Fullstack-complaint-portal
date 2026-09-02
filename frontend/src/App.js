import React, { useEffect, useState } from "react";

function App() {
  const [complaints, setComplaints] = useState([]);
  const [newComplaint, setNewComplaint] = useState("");

  useEffect(() => {
    fetch("http://localhost:8080/api/complaints")
      .then((res) => {
        if (!res.ok) {
          throw new Error("Failed to fetch complaints");
        }
        return res.json();
      })
      .then((data) => {
        setComplaints(data);
      })
      .catch((err) => console.error("Error fetching complaints:", err));
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!newComplaint.trim()) return;

    fetch("http://localhost:8080/api/complaints", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        description: newComplaint,
        category: "General",
        status: "Pending",
      }),
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error("Failed to add complaint");
        }
        return res.json();
      })
      .then((data) => {
        setComplaints((prev) => [...prev, data]);
        setNewComplaint("");
      })
      .catch((err) => console.error("Error submitting complaint:", err));
  };

  const handleDelete = (id) => {
    fetch("http://localhost:8080/api/complaints/" + id, {
      method: "DELETE",
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error("Failed to delete complaint");
        }

        setComplaints((prev) =>
          prev.filter((complaint) => complaint.id !== id)
        );
      })
      .catch((err) => console.error("Error deleting complaint:", err));
  };

  return (
    <div style={{ margin: "2rem", fontFamily: "Arial, sans-serif" }}>
      <h1>Complaint Portal</h1>

      <form onSubmit={handleSubmit} style={{ marginBottom: "1.5rem" }}>
        <input
          type="text"
          value={newComplaint}
          onChange={(e) => setNewComplaint(e.target.value)}
          placeholder="Enter complaint"
          style={{
            padding: "8px",
            marginRight: "8px",
            width: "250px",
            borderRadius: "5px",
            border: "1px solid #ccc",
          }}
        />

        <button
          type="submit"
          style={{
            padding: "8px 16px",
            backgroundColor: "#007bff",
            color: "white",
            border: "none",
            borderRadius: "5px",
            cursor: "pointer",
          }}
        >
          Submit
        </button>
      </form>

      <h2>Complaints List</h2>

      {complaints.length === 0 ? (
        <p>No complaints available.</p>
      ) : (
        <table
          style={{
            borderCollapse: "collapse",
            width: "100%",
            maxWidth: "800px",
            backgroundColor: "#f9f9f9",
          }}
        >
          <thead>
            <tr style={{ backgroundColor: "#007bff", color: "white" }}>
              <th style={{ border: "1px solid #ddd", padding: "8px" }}>
                ID
              </th>
              <th style={{ border: "1px solid #ddd", padding: "8px" }}>
                Category
              </th>
              <th style={{ border: "1px solid #ddd", padding: "8px" }}>
                Description
              </th>
              <th style={{ border: "1px solid #ddd", padding: "8px" }}>
                Status
              </th>
              <th style={{ border: "1px solid #ddd", padding: "8px" }}>
                Action
              </th>
            </tr>
          </thead>

          <tbody>
            {complaints.map((c) => (
              <tr key={c.id}>
                <td style={{ border: "1px solid #ddd", padding: "8px" }}>
                  {c.id}
                </td>

                <td style={{ border: "1px solid #ddd", padding: "8px" }}>
                  {c.category}
                </td>

                <td style={{ border: "1px solid #ddd", padding: "8px" }}>
                  {c.description}
                </td>

                <td style={{ border: "1px solid #ddd", padding: "8px" }}>
                  {c.status}
                </td>

                <td style={{ border: "1px solid #ddd", padding: "8px" }}>
                  <button onClick={() => handleDelete(c.id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default App;
