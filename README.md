# Visual Bible Mapper

A Spring Boot + React app that:

1. Accepts scripture references from both a `.txt` upload and a paste/type textbox.
2. Parses many reference forms (e.g., `Acts 1:12`, `Ac1:12`, `1Jn2:3`, `1 John 3:3`).
3. Generates a full-Bible tiny-text image layout left-to-right by book with breaks between books and a wider break between Old and New Testament.
4. Highlights selected references in bright red and allows click-to-inspect verse text in the sidebar.
5. Saves each generated topic map (name + image path + passage set), and displays them in a list view.

## Run backend

```bash
cd backend
mvn spring-boot:run
```

Backend: `http://localhost:8080`

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`

## How to test manually

1. Start backend and frontend.
2. Open frontend.
3. Enter a topic name, output path, and either upload `sample-passages.txt` or paste references.
4. Click **Generate + Save**.
5. In the saved list view, click the topic.
6. Click bright-red highlighted portions of the image and verify the selected verse appears in the right sidebar.

## Bible dataset resource

A generated Bible visualization dataset is bundled at:

- `backend/src/main/resources/bible/kjv_full.csv`

The application generates a static base image at startup (`data/output/base-bible.png`) and logs generation progress.
