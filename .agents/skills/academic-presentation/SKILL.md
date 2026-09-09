---
name: academic-presentation
description: >-
  Creates professional university-level PowerPoint presentations for technical and academic topics. Use when the user asks to create, design, generate, improve, structure, or revise a PPT, PowerPoint, slide deck, presentation, or academic presentation, especially for CSE, programming, algorithms, software engineering, databases, computer architecture, networking, data science, machine learning, or project presentations.
---

# Academic Presentation Skill

## Purpose

Create high-quality, presentation-ready PowerPoint decks for university students.

The presentation must look intentionally designed, not like AI-generated text placed onto blank slides.

Prioritize:

1. Clear communication
2. Strong visual hierarchy
3. Technical correctness
4. Concise content
5. Professional visual design
6. Easy oral explanation
7. Consistency across the entire deck

---

# Core Workflow

Always follow this workflow unless the user explicitly asks for a different process.

## Phase 1 — Understand

Determine:

- Topic
- Audience
- Course/subject
- Presentation purpose
- Required slide count
- Required duration
- Whether the user needs to explain the topic orally
- Whether the user supplied source material
- Whether citations/references are required
- Whether diagrams, charts, tables, equations, or code are appropriate

If information is missing, make sensible assumptions instead of repeatedly asking questions.

For university presentations, prioritize clarity over excessive sophistication.

---

# Phase 2 — Build the Story

Before creating slides, create an internal slide outline.

A typical technical presentation should follow:

1. Title
2. Introduction / Motivation
3. Problem or Concept
4. Core Explanation
5. How It Works
6. Example / Demonstration
7. Comparison / Analysis
8. Advantages and Limitations
9. Applications / Real-world relevance
10. Conclusion
11. References

Do NOT blindly use all of these.

Choose only the sections that actually help the topic.

Every slide must have one primary purpose.

---

# Phase 3 — Choose Slide Types

Choose the visual structure based on the content.

Use:

- Title slide for introductions
- Section divider for major transitions
- Two-column layout for concept + explanation
- Comparison table for differences
- Flowchart for processes
- Timeline for chronological information
- Architecture diagram for systems
- Pipeline diagram for ML/data workflows
- Algorithm visualization for algorithms
- Entity-relationship diagrams for databases
- Network diagrams for networking
- Block diagrams for computer architecture
- Charts for numerical data
- Code blocks for short code examples
- Cards for grouped concepts
- Highlight boxes for important conclusions

Do not use bullet lists when a diagram, table, or visual structure communicates the information better.

---

# Content Rules

## One Main Idea

Every slide must communicate one main idea.

Do not create slides containing several unrelated concepts.

## Text Density

Avoid paragraphs.

Prefer:

- Short statements
- Keywords
- Small bullet groups
- Diagrams
- Tables
- Visual relationships
- Examples

A slide should support the speaker rather than replace the speaker.

## Bullet Rules

Prefer 3–5 bullets.

Avoid more than 6 bullets unless absolutely necessary.

Keep individual bullets concise.

Do not write textbook paragraphs inside bullet points.

---

# Technical Presentation Rules

For CSE and technical subjects:

## Algorithms

Prefer:

Concept
→ Input
→ Steps
→ Visualization
→ Example
→ Complexity
→ Applications

For algorithm presentations, show the algorithm visually whenever possible.

Example:

Input
↓
Step 1
↓
Step 2
↓
Step 3
↓
Output

Include time and space complexity when relevant.

## Data Structures

Use:

- diagrams
- node representations
- trees
- graphs
- arrays
- operation visualizations

Avoid explaining data structures only through text.

## Database

Prefer:

- ER diagrams
- schemas
- relationships
- SQL examples
- normalization examples
- architecture diagrams

## Computer Architecture

Prefer:

- block diagrams
- CPU diagrams
- memory hierarchy
- instruction flow
- datapaths
- comparison tables

## Networking

Prefer:

- topology diagrams
- packet flow
- protocol layers
- client/server diagrams
- sequence diagrams

## Software Engineering

Prefer:

- lifecycle diagrams
- architecture
- workflows
- UML
- comparison tables

## Data Science / Machine Learning

Prefer:

Dataset
→ Preprocessing
→ Feature Engineering
→ Model
→ Evaluation
→ Prediction

Use charts and visual pipelines when appropriate.

---

# Design System

## Aspect Ratio

Use 16:9 widescreen unless the user specifies otherwise.

## Typography

Use a maximum of 2 font families.

Maintain consistent typography throughout the deck.

Recommended hierarchy:

- Presentation title: large
- Slide title: clearly dominant
- Section heading: medium-large
- Body: highly readable
- Caption: smaller but still readable

Never make body text unnecessarily small just to fit more content.

If content does not fit:

1. Remove unnecessary text.
2. Split the slide.
3. Change the layout.
4. Only then consider reducing font size.

Never solve overcrowding by creating microscopic text.

---

# Visual Design

Use a consistent visual system.

Define before creating the deck:

- Background
- Primary text color
- Secondary text color
- Accent color
- Heading style
- Body style
- Card style
- Diagram style
- Table style

Do not randomly change colors from slide to slide.

Use visual contrast intentionally.

Avoid excessive:

- gradients
- shadows
- glowing effects
- rounded cards
- decorative shapes
- emojis
- stock-style illustrations

Design should support the content.

---

# Academic Style

The presentation should look suitable for:

- University classroom presentations
- Lab presentations
- Assignment submissions
- Project demonstrations
- Technical seminars
- Viva/project defense

Avoid making the deck look like:

- a corporate sales pitch
- a social-media infographic
- a children's presentation
- a template demonstration

---

# Visual Storytelling

Whenever possible, replace text with visual explanations.

For example:

Instead of:

"Client sends a request to the server, which processes the request and returns a response."

Create:

CLIENT
  ↓
HTTP REQUEST
  ↓
SERVER
  ↓
PROCESSING
  ↓
HTTP RESPONSE
  ↓
CLIENT

The audience should understand the concept quickly by looking at the slide.

---

# Diagrams

When creating diagrams:

- Keep them simple.
- Use consistent shapes.
- Use consistent spacing.
- Use arrows to show direction.
- Label relationships clearly.
- Avoid unnecessary decorative elements.

Do not create diagrams that are visually impressive but technically ambiguous.

Technical correctness is more important than aesthetics.

---

# Tables

Use tables for:

- comparisons
- feature differences
- advantages/disadvantages
- algorithm complexity
- technology comparisons

Keep tables concise.

Never put an entire textbook table onto one slide.

---

# Code

When displaying code:

- Show only the relevant section.
- Use syntax highlighting when available.
- Keep font readable.
- Highlight the important lines.
- Explain the code visually or with short annotations.

Never put an entire source file on a slide.

---

# Images

Use images only when they add meaningful value.

Prefer:

- technical diagrams
- architecture diagrams
- meaningful illustrations
- charts
- screenshots when demonstrating software

Avoid generic decorative stock images.

If an image does not help explain the topic, remove it.

---

# Citations

When external information is used:

- Keep citations concise on relevant slides.
- Add a References slide when appropriate.
- Do not invent sources.
- Do not fabricate statistics.
- Do not fabricate research papers.
- Verify important factual claims when possible.

For academic presentations, distinguish between:

- information from provided material
- general knowledge
- externally researched information

---

# Speaker Notes

If the presentation is intended for oral explanation, create useful speaker notes when appropriate.

Speaker notes should explain:

- What the presenter should say
- Important definitions
- How to explain diagrams
- Examples
- Transitions between ideas
- Possible questions from the teacher

Do not simply repeat the slide text.

---

# Slide Count

Do not artificially increase the number of slides.

If the user requests 10 slides, target approximately 10 useful slides.

If the content requires fewer slides, keep it concise.

If the topic genuinely requires more slides, explain why.

---

# Presentation Timing

If presentation duration is provided, estimate approximately:

- Title: 15–30 sec
- Normal concept slide: 45–90 sec
- Complex technical slide: 1–2 min
- Conclusion: 30–45 sec

Do not overload a slide with more information than can reasonably be explained in the available time.

---

# PPTX Generation

When generating an actual `.pptx`:

1. Create the presentation structure.
2. Generate the slides.
3. Apply the visual design consistently.
4. Add diagrams and visual elements.
5. Add speaker notes if requested/appropriate.
6. Save the `.pptx`.
7. Render the presentation to images/PDF if the environment supports it.
8. Inspect the rendered slides.
9. Fix visual problems.
10. Render again.
11. Only then consider the presentation finished.

---

# Visual Quality Assurance

After generating the presentation, inspect every slide.

Check:

### Layout

- Nothing overlaps.
- Nothing is outside the slide.
- Margins are consistent.
- Objects are aligned.
- Spacing is intentional.

### Text

- No text overflow.
- No clipped text.
- Fonts are readable.
- Titles are consistent.
- No giant paragraphs.

### Visuals

- Images are not distorted.
- Diagrams are understandable.
- Arrows point correctly.
- Charts are readable.
- Tables fit comfortably.

### Consistency

- Same typography system.
- Same spacing system.
- Same visual language.
- Same title positioning where appropriate.
- Same footer/reference style.

### Academic Quality

- Technical claims are correct.
- Examples are relevant.
- Terminology is correct.
- References are included when necessary.

---

# Error Handling

If a slide is overcrowded:

Do NOT simply reduce all font sizes.

Instead:

1. Remove unnecessary content.
2. Convert text into a diagram.
3. Split the slide.
4. Change the layout.
5. Reduce font size only as a final measure.

If a slide looks empty:

Do NOT add random decoration.

Instead:

- improve hierarchy
- add a relevant diagram
- add an example
- increase useful visual spacing
- restructure the content

---

# User Interaction

When the user gives a simple request such as:

"Make a PPT about TCP vs UDP."

Do not immediately generate random slides.

First internally determine:

- audience
- likely academic level
- appropriate slide count
- narrative
- visuals
- technical depth

Then execute.

If the user provides:

- PDF
- DOCX
- notes
- syllabus
- assignment question
- research paper
- website
- existing PPT

Treat that material as the primary source.

Do not unnecessarily replace the user's provided content with unrelated information.

---

# Final Deliverable

When finished, provide:

1. The generated `.pptx`
2. A short summary of the deck
3. Slide count
4. Any important assumptions
5. Any external sources used

The presentation must be editable as a normal PowerPoint presentation whenever technically possible.

---

# Golden Rule

The goal is NOT:

"Put as much information as possible onto slides."

The goal is:

"Make the audience understand the topic quickly, while giving the presenter enough structure to explain it confidently."

Content > decoration.

Clarity > complexity.

Technical correctness > visual effects.

Visual storytelling > walls of text.

Always verify the final presentation before declaring it finished.
