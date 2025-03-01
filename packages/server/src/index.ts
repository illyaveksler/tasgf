import express from "express";
import { Questionnaires } from "./questionnaires/questionnaires.ts";

const PORT = 80;
const app = express();

app.use(express.json());

// Create a new questionnaire
app.post("/questionnaires", (req, res) => {
  const { title, description, questions } = req.body;
  try {
    const newQuestionnaire = Questionnaires.createQuestionnaire(title, description, questions);
    res.status(201).json(newQuestionnaire);
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

// Retrieve all questionnaires (summary)
app.get("/questionnaires", (req, res) => {
  const allQuestionnaires = Questionnaires.getAllQuestionnaires();
  res.json(allQuestionnaires);
});

// Retrieve a specific questionnaire (full details)
app.get("/questionnaires/:questionnaireId", (req, res): void => {
  const questionnaire = Questionnaires.getQuestionnaire(parseInt(req.params.questionnaireId));
  if (!questionnaire) {
    res.status(404).json({ error: "Questionnaire not found." });
    return;
  }
  res.json(questionnaire);
});

// Submit answers for a specific questionnaire
app.post("/questionnaires/:questionnaireId/answers", (req, res) => {
  const questionnaireId = req.params.questionnaireId;
  const { answers } = req.body;
  try {
    const submission = Questionnaires.submitAnswers(parseInt(questionnaireId), answers);
    res.status(201).json(submission);
  } catch (error: any) {
    res.status(400).json({ error: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});
