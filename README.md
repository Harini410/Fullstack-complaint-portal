
# Fullstack Complaint Portal

Full-stack complaint management system built with Spring Boot, Hibernate, MySQL (backend) and React.js (frontend).  
The portal allows users to securely submit, track, and manage complaints while improving internal workflow efficiency.

---

## Features
- Secure Authentication – Role-based login for users and admins using Spring Security  
- Complaint Tracking – Centralized system to log, view, and update complaint statuses  
- Efficient Workflow – Reduced complaint resolution time by approximately 30%  
- Database Integration – Persistent storage using MySQL with Hibernate ORM  
- Modern UI – Responsive React.js frontend connected to Spring Boot REST APIs  

---

## Tech Stack

Backend:
- Java, Spring Boot, Hibernate  
- Spring Security  
- RESTful APIs  
- MySQL  

Frontend:
- React.js  
- Axios for API calls  
- CSS / Bootstrap  

Build Tools:
- Maven (backend)  
- npm / yarn (frontend)  

---

## Setup & Installation

### Clone the repository
\`\`\`
git clone https://github.com/Harini410/Fullstack-complaint-portal.git
cd Fullstack-complaint-portal
\`\`\`

### Backend Setup
\`\`\`
cd backend
mvn clean install
\`\`\`

Configure MySQL database in `src/main/resources/application.properties`:
\`\`\`
spring.datasource.url=jdbc:mysql://localhost:3306/complaintdb
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
\`\`\`

Run the backend:
\`\`\`
mvn spring-boot:run
\`\`\`

### Frontend Setup
\`\`\`
cd frontend
npm install
npm start
\`\`\`

Frontend runs at http://localhost:3000  
Backend runs at http://localhost:8080  

---

## Project Structure
\`\`\`
Fullstack-complaint-portal/
├── backend/
│   ├── src/main/java/       # Controllers, Entities, Repositories, Security
│   ├── src/main/resources/  # application.properties
│   └── pom.xml
├── frontend/
│   ├── src/components/      # React components
│   ├── package.json
│   └── public/
└── README.md
\`\`\`

-----

## Future Enhancements
- Email/SMS notifications for status updates  
- Analytics dashboard for complaint insights  
- Deployment on AWS / Docker  

---

## Contributing
Fork the repository and submit a pull request. Contributions are welcome.

---

## License
MIT License

---

## Author
Harini L  
GitHub: https://github.com/Harini410  
LinkedIn: https://www.linkedin.com/in/harini-lakshmanan-04
