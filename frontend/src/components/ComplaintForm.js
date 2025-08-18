import React, { useState } from "react";

function ComplaintForm({ onComplaintSubmit }) {
  const [complaint, setComplaint] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (complaint.trim()) {
      try {
        const response = await fetch("http://localhost:8080/api/complaints", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            category: "General",
            description: complaint,
            status: "Pending",
          }),
        });

        if (!response.ok) {
          throw new Error("Failed to submit complaint");
        }

        const data = await response.json();
        onComplaintSubmit(data);
        setComplaint("");
      } catch (error) {
        console.error("Error submitting complaint:", error);
      }
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="Enter complaint"
        value={complaint}
        onChange={(e) => setComplaint(e.target.value)}
        required
      />
      <button type="submit">Submit</button>
    </form>
  );
}

export default ComplaintForm;
