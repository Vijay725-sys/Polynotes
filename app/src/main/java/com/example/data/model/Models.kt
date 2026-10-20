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

data class SystemHealthCheckItem(
    val name: String = "",
    val passed: Boolean = true,
    val detail: String = ""
)

data class SystemHealthReport(
    val allHealthy: Boolean = true,
    val proxyEndpointUrl: String = "",
    val checks: List<SystemHealthCheckItem> = emptyList()
)

/**
 * Shared DTO for syncing student profile & progress with the Backend Proxy (`/api/v1/profile`).
 */
data class UserProfileDoc(
    val id: String = "",
    val userId: String = "default_student",
    val name: String = "Rahul Sharma",
    val college: String = "Govt. Polytechnic Dehradun",
    val board: String = "UBTER",
    val branch: String = "CIVIL",
    val semester: Long = 3L,
    val bookmarkedIds: List<String> = listOf("res_01", "res_07", "res_15"),
    val completedTopics: List<String> = listOf(
        "Surveying Fundamental Principles",
        "Pull, Temperature & Sag Corrections",
        "IS 10262 Step-by-Step Flowchart"
    ),
    val darkMode: Boolean = false
) {
    fun toUserProfile(): UserProfile {
        val parsedBoard = try {
            TechnicalBoard.valueOf(board)
        } catch (_: Exception) {
            TechnicalBoard.UBTER
        }
        val parsedBranch = try {
            EngineeringBranch.valueOf(branch)
        } catch (_: Exception) {
            EngineeringBranch.CIVIL
        }
        return UserProfile(
            name = name.ifBlank { "Rahul Sharma" },
            college = college.ifBlank { "Govt. Polytechnic Dehradun" },
            board = parsedBoard,
            branch = parsedBranch,
            semester = semester.toInt().coerceIn(1, 6)
        )
    }
}

/**
 * Shared DTO for syncing community study resources with the Backend Proxy (`/api/v1/resources`).
 */
data class CloudStudyResource(
    val id: String = "",
    val userId: String = "default_student",
    val subjectId: String = "ubter_ce_301",
    val subjectName: String = "Basic Surveying",
    val title: String = "",
    val unit: Long = 1L,
    val type: String = "NOTES",
    val documentUrl: String = "https://www.ubter.in",
    val source: String = "Student Contributor",
    val reviewStatus: String = "UNDER_REVIEW",
    val uploadedAt: String = "Oct 2026",
    val semester: Long = 3L,
    val description: String = "",
    val topicsCovered: List<String> = emptyList(),
    val pageCountOrLength: String = "Community Submission",
    val fileFormat: String = "PDF / Web Link"
) {
    fun toStudyResource(): StudyResource {
        val parsedType = try {
            ResourceType.valueOf(type)
        } catch (_: Exception) {
            ResourceType.NOTES
        }
        val parsedStatus = try {
            ReviewStatus.valueOf(reviewStatus)
        } catch (_: Exception) {
            ReviewStatus.UNDER_REVIEW
        }
        return StudyResource(
            id = id.ifBlank { "proxy_res" },
            subjectId = subjectId,
            subjectName = subjectName,
            title = title,
            unit = unit.toInt().coerceIn(1, 6),
            type = parsedType,
            documentUrl = documentUrl,
            source = source,
            reviewStatus = parsedStatus,
            uploadedAt = uploadedAt,
            semester = semester.toInt().coerceIn(1, 6),
            description = description,
            topicsCovered = topicsCovered,
            pageCountOrLength = pageCountOrLength,
            fileFormat = fileFormat
        )
    }
}
