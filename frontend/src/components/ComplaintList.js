import React, { useEffect, useState } from "react";

function ComplaintList() {
  const [complaints, setComplaints] = useState([]);

  useEffect(() => {
    fetch("http://localhost:8080/api/complaints")
      .then((res) => res.json())
      .then((data) => {
        console.log("Fetched complaints:", data); // ✅ check if data is coming
        setComplaints(data);
      })
      .catch((err) => console.error("Error fetching complaints:", err));
  }, []);

  return (
    <div>
      <h2>Complaints List</h2>
      <table border="1" cellPadding="8">
        <thead>
          <tr>
            <th>ID</th>
            <th>Category</th>
            <th>Description</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {complaints.length > 0 ? (
            complaints.map((complaint) => (
              <tr key={complaint.id}>
                <td>{complaint.id}</td>
                <td>{complaint.category}</td>
                <td>{complaint.description}</td>
                <td>{complaint.status}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="4">No complaints available.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

export default ComplaintList;
