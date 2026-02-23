# Visual Bible Mapper

A Spring Boot + React app that:

1. Accepts scripture references from both a `.txt` upload and an editable memo textbox.
2. Auto-loads selected file content into the memo field for in-browser editing.
3. Parses many reference forms, including chapter and cross-chapter ranges (examples: `Acts 1:12`, `Ac1:12`, `Romans 11`, `Jeremiah 30:3-34:4`, `1Jn2:3`).
4. Generates a full-Bible tiny-text image laid out as Bible-style pages (2 text columns per page, each page ~64px wide), arranged top-to-bottom with up to 20 pages per vertical page-column.
5. Highlights selected references in bright red and allows click-to-inspect verse text in the sidebar.
6. Saves each generated topic map and displays saved maps in a list view.

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
3. Choose a `.txt` file and confirm its text appears in the memo field.
4. Edit/add references in the memo field (e.g., `Romans 11` and `Jeremiah 30:3-34:4`).
5. Click **Generate + Save**.
6. Select a topic from the saved list.
7. Use the zoom control (default ~22% zoomed-out) and verify viewport supports both horizontal and vertical scrolling.
8. Click bright-red highlighted regions and verify selected verse text appears in the sidebar.

## Bible dataset resource

A generated Bible visualization dataset is bundled at:

- `backend/src/main/resources/bible/kjv_full.csv`

The application generates a static base image at startup (`data/output/base-bible.png`) and logs generation progress.
