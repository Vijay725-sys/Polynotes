package com.example.data.model

enum class TechnicalBoard(val code: String, val displayName: String, val state: String) {
    UBTER("UBTER", "UBTER (Roorkee)", "Uttarakhand"),
    BTEUP("BTEUP", "BTEUP (Lucknow)", "Uttar Pradesh"),
    HSBTE("HSBTE", "HSBTE (Panchkula)", "Haryana"),
    MSBTE("MSBTE", "MSBTE (Mumbai)", "Maharashtra")
}

enum class EngineeringBranch(val code: String, val displayName: String, val shortName: String) {
    CIVIL("CIVIL", "Civil Engineering", "Civil"),
    MECHANICAL("MECH", "Mechanical Engineering", "Mechanical"),
    ELECTRICAL("ELEC", "Electrical Engineering", "Electrical"),
    CSE("CSE", "Computer Science & Engg", "CSE")
}

enum class ResourceType(val displayName: String, val iconLabel: String) {
    NOTES("Notes", "📄"),
    PRACTICALS("Practicals", "🔬"),
    PYQS("PYQs", "📑"),
    VIVA("Viva Voce", "🎙️")
}

enum class ReviewStatus(val displayName: String) {
    FACULTY_VERIFIED("Faculty Verified"),
    PEER_REVIEWED("Peer Reviewed"),
    UNDER_REVIEW("Under Review")
}

data class SubjectUnit(
    val unitNumber: Int,
    val title: String,
    val summary: String,
    val keyTopics: List<String>
)

data class Subject(
    val id: String,
    val board: String = "UBTER",
    val branch: String = "CIVIL",
    val semester: Int,
    val name: String,
    val code: String,
    val unitCount: Int,
    val description: String,
    val units: List<SubjectUnit>
)

data class StudyResource(
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val title: String,
    val unit: Int,
    val type: ResourceType,
    val documentUrl: String,
    val source: String,
    val reviewStatus: ReviewStatus,
    val uploadedAt: String,
    val semester: Int,
    val description: String,
    val topicsCovered: List<String>,
    val pageCountOrLength: String,
    val fileFormat: String = "PDF Document"
)

data class UserProfile(
    val name: String = "Rahul Sharma",
    val college: String = "Govt. Polytechnic Dehradun",
    val board: TechnicalBoard = TechnicalBoard.UBTER,
    val branch: EngineeringBranch = EngineeringBranch.CIVIL,
    val semester: Int = 3
)
