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
    val unitNumber: Int = 1,
    val title: String = "",
    val summary: String = "",
    val keyTopics: List<String> = emptyList()
)

data class Subject(
    val id: String = "",
    val board: String = "UBTER",
    val branch: String = "CIVIL",
    val semester: Int = 3,
    val name: String = "",
    val code: String = "",
    val unitCount: Int = 5,
    val description: String = "",
    val units: List<SubjectUnit> = emptyList()
)

data class StudyResource(
    val id: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val title: String = "",
    val unit: Int = 1,
    val type: ResourceType = ResourceType.NOTES,
    val documentUrl: String = "",
    val source: String = "",
    val reviewStatus: ReviewStatus = ReviewStatus.UNDER_REVIEW,
    val uploadedAt: String = "",
    val semester: Int = 3,
    val description: String = "",
    val topicsCovered: List<String> = emptyList(),
    val pageCountOrLength: String = "",
    val fileFormat: String = "PDF Document"
)

data class UserProfile(
    val name: String = "Rahul Sharma",
    val college: String = "Govt. Polytechnic Dehradun",
    val board: TechnicalBoard = TechnicalBoard.UBTER,
    val branch: EngineeringBranch = EngineeringBranch.CIVIL,
    val semester: Int = 3
)
