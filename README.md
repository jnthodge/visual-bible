# Visual Bible Mapper

A Spring Boot + React app that:

1. Accepts a `.txt` list of scripture references (one per line, e.g. `John 3:1`).
2. Generates a very large image representing the full Bible chapter flow and highlights matching passages in red.
3. Saves the image to a user-provided output path with a user-provided name.
4. Lists saved maps by name, displays the selected image, and shows highlighted verse text on hover.

## Run backend

```bash
cd backend
mvn spring-boot:run
```

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: `http://localhost:5173`
Backend: `http://localhost:8080`

## Passage file format

Plain text, one passage per line:

```txt
Genesis 1:1
Psalms 23:1
John 3:1
Revelation 22:1
```

Only exact `Book Chapter:1` entries are highlighted in this generated overview image.
