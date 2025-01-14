use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct PaginatedResponse<T> {
    pub content: Vec<T>,
    pub number: u32,
    pub size: u32,
    pub first: bool,
    pub last: bool,
    pub total_elements: u64,
    pub total_pages: u32,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct SubmissionResponse {
    pub id: u64,
    pub code: String,
    pub language_id: u64,
    pub stdin: String,
    pub expected_output: String,
    pub solution_id: u64,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct SolutionResponse {
    pub id: u64,
    pub user_id: u64,
    pub exercise_id: u64,
    pub status: u32,
    pub points_earned: u32,
    pub submissions: Vec<SubmissionResponse>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Exercise {
    pub id: u64,
    pub slug: String,
    pub title: String,
    pub points: u32,
    pub instructions: String,
    pub hints: String,
    pub lesson_id: u64,
    pub github_path: String,
    pub last_github_sync: String,
    pub solution_responses: Vec<SolutionResponse>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Lesson {
    pub id: u64,
    pub name: String,
    pub content: String,
    pub credits: u32,
    pub slug: String,
    pub module_id: u64,
    pub module_name: String,
    pub exercise_ids: Vec<u64>,
    pub created_at: String,
    pub updated_at: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Roadmap {
    pub id: u64,
    pub title: String,
    pub description: String,
    pub slug: String,
    pub image_url: Option<String>,
    pub tag_names: Vec<String>,
    pub is_published: bool,
    pub course_slugs: Vec<String>,
    pub created_at: String,
    pub updated_at: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Module {
    pub id: u64,
    pub name: String,
    pub description: String,
    pub slug: String,
    pub course_id: u64,
    pub course_name: String,
    pub image_url: Option<String>,
    pub lesson_ids: Vec<u64>,
    pub created_at: String,
    pub updated_at: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ExerciseDetails {
    #[serde(flatten)]
    pub exercise: Exercise,
    pub language: String,
    pub initial_code: String,
    pub test_code: String,
    pub lesson_name: String,
    pub file_name: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Course {
    pub id: u64,
    pub name: String,
    pub description: String,
    pub slug: String,
    pub image_url: Option<String>,
    pub roadmap_slugs: Vec<String>,
    pub tag_names: Vec<String>,
    pub module_ids: Vec<u64>,
    pub created_at: String,
    pub updated_at: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct RoadmapDetails {
    #[serde(flatten)]
    pub roadmap: Roadmap,
    pub courses: Vec<Course>,
}

pub type PaginatedExercises = PaginatedResponse<Exercise>;
pub type PaginatedLessons = PaginatedResponse<Lesson>;
pub type PaginatedRoadmaps = PaginatedResponse<Roadmap>;
pub type PaginatedModules = PaginatedResponse<Module>;
