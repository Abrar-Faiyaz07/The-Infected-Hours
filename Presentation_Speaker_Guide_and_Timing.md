# Final Project Presentation Guide & Speaker Scripts
**Course**: CSE 4408 – System Analysis and Design Lab (SDA Lab)  
**Project**: Disease Spread Prevention System  
**Group**: Go With The Flow (Section 1A)  
**Total Allocated Time**: 8 Minutes Maximum (Strict)  
**Total Slides**: 15 Slides (16:9 Widescreen)  

---

## Team Roster & Speaking Distribution

| Presenter | Student ID | Assigned Slides | Section Focus | Allocated Time |
| :--- | :--- | :--- | :--- | :--- |
| **Ahmed Samin Yasar** | 230041113 | Slides 1 – 4 | Title, Organizational Context, Problem & Feasibility | **2:00 mins** (0:00 – 2:00) |
| **Mahir Labib** | 230041139 | Slides 5 – 7 | Info Gathering, Key Findings & System Requirements | **1:50 mins** (2:00 – 3:50) |
| **Abrar Faiyaz** | 230041143 | Slides 8 – 10 | System Modeling, Context Diagram, Level-1 DFD & ERD | **1:45 mins** (3:50 – 5:35) |
| **Ashique Khan** | 230041153 | Slides 11 – 15 | UI Prototypes, Analytics Dashboard, Solution & Viva | **2:15 mins** (5:35 – 7:50) |

---

## Slide-by-Slide Speaking Script & Timing

### Part 1: Introduction, Problem & Feasibility (Presenter: Ahmed Samin Yasar)
- **Slide 1: Title Slide (0:00 – 0:30 | 30s)**
  - *Script*: "Good morning respected faculty and fellow peers. We are group 'Go With The Flow' from Section 1A. Today, we are presenting our final System Analysis and Design project: The **Disease Spread Prevention System**—an early-detection, clinical verification, and campus outbreak control platform designed for the Islamic University of Technology."
- **Slide 2: Organizational Context (0:30 – 1:00 | 30s)**
  - *Script*: "IUT is a dense residential institution where thousands of students share dormitories, dining halls, and classrooms. The IUT Medical Center currently acts as the central care facility, but fragmented record-keeping creates lag between the first localized infection and institutional containment."
- **Slide 3: Problem Statement & Objectives (1:00 – 1:30 | 30s)**
  - *Script*: "Our problem analysis revealed 4 major bottlenecks: manual physical logbooks, delayed outbreak detection, unstructured external medical slips, and slow communication. Our objectives are clear: digital symptom logging, a rapid verification queue, automated cluster detection, and targeted alerts."
- **Slide 4: Feasibility Study (1:30 – 2:00 | 30s)**
  - *Script*: "Our feasibility study confirms that the system is technically lightweight and responsive, economically viable with zero additional capital expenditure, and operationally frictionless with rapid doctor triage workflows. I will now hand over to Mahir Labib."

---

### Part 2: Research Findings & Requirements (Presenter: Mahir Labib)
- **Slide 5: Information Gathering Methodology (2:00 – 2:35 | 35s)**
  - *Script*: "Thank you, Samin. In Labs 5 and 6, we employed both interactive and unobtrusive techniques. We conducted semi-structured interviews with chief medical officers and hall provosts, ran cross-batch student surveys, and sampled 6 months of historical clinical logbooks."
- **Slide 6: Key Findings & Pain Points (2:35 – 3:10 | 35s)**
  - *Script*: "Our research revealed distinct stakeholder pain points: students in dorms delay reporting mild fevers due to physical clinic fatigue; doctors struggle with unverified external doctor notes; and campus authorities operate blind to emerging dorm clusters."
- **Slide 7: Functional & Non-Functional Requirements (3:10 – 3:50 | 40s)**
  - *Script*: "We established 5 core functional requirements: student profile management, symptom ingestion, doctor verification queues, dynamic cohort notifications, and outbreak analytics. These are reinforced by HIPAA-aligned confidentiality, sub-second input validation, and high availability. Next, Abrar Faiyaz will discuss our System Modeling."

---

### Part 3: System Modeling & Architecture (Presenter: Abrar Faiyaz)
- **Slide 8: Context Diagram (DFD Level 0) (3:50 – 4:25 | 35s)**
  - *Script*: "Thank you, Mahir. Process 0.0 defines our system boundary, interfacing with 4 external entities: Students, Medical Staff, Dormitory Authorities, and University Authorities. The system transforms student-reported health symptoms into clinical triage data and strategic outbreak alerts."
- **Slide 9: Logical DFD Level 1 (4:25 – 5:00 | 35s)**
  - *Script*: "At Level 1, we decomposed the architecture into 5 discrete sub-processes: Profile Management (1.0), Symptom Ingestion (2.0), Medical Verification (3.0), Analytics Engine (4.0), and Notification Dispatch (5.0), establishing an unbroken data pipeline."
- **Slide 10: Data Stores & Architecture (5:00 – 5:35 | 35s)**
  - *Script*: "Our storage is segregated into three logical stores: D1 for student identity and room coordinates, D2 for clinical records and verified prescriptions, and D3 for aggregated epidemiological metrics. This maintains medical data privacy while supporting high-speed analytics. Ashique Khan will now showcase our working prototypes."

---

### Part 4: Prototypes, Analytics & Solution (Presenter: Ashique Khan)
- **Slide 11: Student Input Prototypes (5:35 – 6:10 | 35s)**
  - *Script*: "Thank you, Abrar. In Lab 12, we developed working high-fidelity prototypes. Screen 1 handles Student Registration with dynamic UI controls that show dorm fields only for residential students. Screen 2 allows ill students to log symptom severity, tick checklist items, and drag-and-drop external medical certificates with instant tracking codes."
- **Slide 12: Medical & Notification Prototypes (6:10 – 6:45 | 35s)**
  - *Script*: "Screen 3 provides doctors with a dual-pane workspace, reducing review time from 10 minutes of manual paperwork to 60 seconds of digital review. Screen 4 allows authorities to dispatch cohort-filtered notifications targeting specific dorm floors or the whole campus."
- **Slide 13: System Outputs & Heatmap (6:45 – 7:15 | 30s)**
  - *Script*: "In Lab 11, we designed system outputs centered on the Outbreak Analytics Dashboard. It provides a real-time spatial campus heatmap with automated threshold alarms when 3+ cases occur in a hall wing within 48 hours, alongside exportable executive briefings."
- **Slide 14: Proposed Solution & Strategic Impact (7:15 – 7:45 | 30s)**
  - *Script*: "Our solution eliminates paper backlogs, transforms campus disease containment from reactive to proactive, prevents forged sick leaves, and ensures academic continuity for IUT."
- **Slide 15: Conclusion & Viva Defense (7:45 – 8:00 | 15s)**
  - *Script*: "In conclusion, we have fulfilled all SDA lifecycle phases across Labs 5 through 12. Our team is now ready for your questions and evaluation. Thank you!"

---

## Anticipated Viva / Teacher Defense Questions & Sample Answers

1. **Q: Why separate D2 (Medical Records) and D3 (Analyzed Outbreak Data)?**  
   - *A*: To maintain HIPAA-level privacy and performance. D2 contains sensitive personal health data accessible only to doctors. D3 holds anonymized, pre-aggregated statistical metrics so dashboards and administrative users can query trends without accessing private student medical records.

2. **Q: How does the system prevent fake medical leave submissions?**  
   - *A*: Students cannot self-approve sick leave. External certificates enter an unverified triage queue in Screen 3 where a certified IUT medical officer must inspect the document, enter clinical remarks, and cryptographically sign/verify the record before it becomes valid.

3. **Q: What happens if a student is a Non-Resident?**  
   - *A*: Our input form features dynamic conditional controls: selecting 'Non-Resident' hides dormitory and room number fields and marks off-campus contact addresses as mandatory.
