package com.example.data.sample

import com.example.data.model.ResourceType
import com.example.data.model.ReviewStatus
import com.example.data.model.StudyResource
import com.example.data.model.Subject
import com.example.data.model.SubjectUnit

object SampleData {

    val sampleSubjects: List<Subject> = listOf(
        Subject(
            id = "ubter_ce_301",
            board = "UBTER",
            branch = "CIVIL",
            semester = 3,
            name = "Surveying - I",
            code = "CE-301",
            unitCount = 5,
            description = "Basic principles, chain surveying, compass traversing, levelling, contouring, and plane table surveying.",
            units = listOf(
                SubjectUnit(1, "Introduction & Chain Surveying", "Principles of surveying, chaining errors, ranging, and cross-staff surveying.", listOf("Principles of Surveying", "Well Conditioned Triangles", "Chaining Errors & Corrections", "Obstacles in Chaining")),
                SubjectUnit(2, "Compass Traversing", "Prismatic & Surveyor compass, fore bearing, back bearing, and local attraction.", listOf("Prismatic Compass Components", "Fore & Back Bearing Conversion", "Local Attraction Correction", "Traverse Adjustment")),
                SubjectUnit(3, "Levelling & Contouring", "Dumpy level, collimation method, rise & fall method, and contour plotting.", listOf("Height of Collimation vs Rise & Fall", "Curvature & Refraction Correction", "Characteristics of Contours", "Direct & Indirect Contouring")),
                SubjectUnit(4, "Plane Table Surveying", "Equipment, orientation, radiation, intersection, and traversing.", listOf("Orientation by Back Sighting", "Two Point & Three Point Problem", "Radiation Method Steps", "Errors in Plane Tabling")),
                SubjectUnit(5, "Minor Instruments", "Abney level, pantagraph, sextant, and digital planimeter.", listOf("Abney Level Use", "Planimeter Zero Circle", "Hand Level Working", "Box Sextant Basics"))
            )
        ),
        Subject(
            id = "ubter_ce_302",
            board = "UBTER",
            branch = "CIVIL",
            semester = 3,
            name = "Concrete Technology",
            code = "CE-302",
            unitCount = 5,
            description = "Properties of cement, aggregates, water-cement ratio, concrete mix design, workability, and quality control.",
            units = listOf(
                SubjectUnit(1, "Cement & Raw Materials", "Manufacture, chemical composition, hydration, and field/lab testing.", listOf("Bogues Compounds", "Setting Time Tests", "Soundness Test", "Grades of OPC")),
                SubjectUnit(2, "Aggregates & Water-Cement Ratio", "Classification, fineness modulus, bulking of sand, and Abrams law.", listOf("Fineness Modulus Calculation", "Bulking of Sand Effect", "Water-Cement Ratio Law", "Aggregate Crushing Value")),
                SubjectUnit(3, "Workability & Fresh Concrete", "Slump test, compaction factor test, segregation, and bleeding.", listOf("Slump Test Procedure", "Compacting Factor Factor", "Segregation Causes & Cure", "Bleeding Control")),
                SubjectUnit(4, "Concrete Mix Proportioning", "IS 10262 method, target mean strength, and quality control grades.", listOf("Target Mean Strength Formula", "Water Content Estimation", "Coarse-to-Fine Aggregate Ratio", "Trial Mix Adjustments")),
                SubjectUnit(5, "Special Concretes & Curing", "Ready mix concrete, fiber reinforced concrete, and curing methods.", listOf("Steam & Membrane Curing", "Self Compacting Concrete", "Underwater Concreting", "Hot Weather Concreting Precautions"))
            )
        ),
        Subject(
            id = "ubter_ce_303",
            board = "UBTER",
            branch = "CIVIL",
            semester = 3,
            name = "Hydraulics & Fluid Mechanics",
            code = "CE-303",
            unitCount = 4,
            description = "Fluid pressure, hydrostatics, buoyancy, flow measurement, Bernoulli's theorem, and pipe friction.",
            units = listOf(
                SubjectUnit(1, "Hydrostatics & Fluid Properties", "Density, viscosity, surface tension, Pascal's law, and manometers.", listOf("Dynamic vs Kinematic Viscosity", "Piezometer & U-Tube Manometer", "Total Pressure on Submerged Plates", "Center of Pressure")),
                SubjectUnit(2, "Fluid Kinematics & Dynamics", "Types of flow, continuity equation, Bernoulli's theorem and venturimeter.", listOf("Laminar vs Turbulent Flow", "Bernoulli Equation Derivation", "Venturimeter Coefficient of Discharge", "Pitot Tube Velocity")),
                SubjectUnit(3, "Flow Through Orifices & Notches", "Hydraulic coefficients, rectangular notch, and V-notch discharge.", listOf("Cc, Cv, and Cd Relationship", "Rectangular Notch Formula", "V-Notch Discharge Formula", "Submerged Orifice")),
                SubjectUnit(4, "Flow Through Pipes", "Darcy-Weisbach equation, hydraulic gradient line, and total energy line.", listOf("Darcy Friction Factor", "Minor Losses in Pipes", "HGL vs TEL Plotting", "Pipes in Series and Parallel"))
            )
        ),
        Subject(
            id = "ubter_ce_401",
            board = "UBTER",
            branch = "CIVIL",
            semester = 4,
            name = "Surveying - II & Tachometry",
            code = "CE-401",
            unitCount = 4,
            description = "Theodolite traversing, tachometric surveying, curve setting, and modern total station introduction.",
            units = listOf(
                SubjectUnit(1, "Theodolite Traversing", "Transit theodolite parts, temporary adjustments, measurement of angles, and Gale's traverse table.", listOf("Repetition & Reiteration Method", "Temporary Adjustments", "Consecutive & Independent Coordinates", "Balancing Traverse")),
                SubjectUnit(2, "Tachometric Surveying", "Stadia method, movable hair method, and determination of constants.", listOf("Stadia Hair Principle", "Distance & Elevation Formulae", "Determining K and C Constants", "Anallatic Lens")),
                SubjectUnit(3, "Curve Setting", "Simple circular curves, elements, Rankine's deflection angle method.", listOf("Degree of Curve vs Radius", "Rankine Tangential Angles", "Offsets from Long Chord", "Transition Curve Basics")),
                SubjectUnit(4, "Total Station & GPS Intro", "Electronic Distance Measurement (EDM), GIS, and digital levels.", listOf("EDM Working Principle", "Total Station Station Setup", "GPS Satellites & Coordinates", "Data Download & Mapping"))
            )
        ),
        Subject(
            id = "ubter_ce_402",
            board = "UBTER",
            branch = "CIVIL",
            semester = 4,
            name = "Soil Mechanics & Foundation",
            code = "CE-402",
            unitCount = 4,
            description = "Soil classification, permeability, shear strength, bearing capacity, and shallow/deep foundation design.",
            units = listOf(
                SubjectUnit(1, "Physical Properties of Soil", "3-phase system, void ratio, porosity, water content, and Atterberg limits.", listOf("3-Phase Diagram Derivations", "Liquid & Plastic Limit Determination", "IS Soil Classification (USCS)", "Particle Size Distribution")),
                SubjectUnit(2, "Permeability & Compaction", "Darcy's law, constant/falling head permeameter, and Standard Proctor test.", listOf("Constant vs Falling Head Test", "Factors Affecting Permeability", "OMC and MDD Compaction Curve", "Field Compaction Equipment")),
                SubjectUnit(3, "Shear Strength of Soil", "Mohr-Coulomb criteria, direct shear test, and unconfined compression test.", listOf("Cohesion and Angle of Internal Friction", "Direct Shear Apparatus", "Unconfined Compression Test", "Vane Shear for Clays")),
                SubjectUnit(4, "Bearing Capacity & Foundations", "Terzaghi's bearing capacity theory, plate load test, and pile foundations.", listOf("Terzaghi BC Factors Nc, Nq, Nγ", "Plate Load Test Procedure", "Raft vs Isolated Footing", "End Bearing vs Friction Piles"))
            )
        ),
        Subject(
            id = "ubter_ce_403",
            board = "UBTER",
            branch = "CIVIL",
            semester = 4,
            name = "Structural Mechanics (SOM)",
            code = "CE-403",
            unitCount = 4,
            description = "Direct stress and strain, SFD & BMD for simply supported and cantilever beams, bending and shear stresses.",
            units = listOf(
                SubjectUnit(1, "Stress, Strain & Elastic Constants", "Hooke's law, modulus of elasticity, Poisson's ratio, and thermal stress.", listOf("Stress-Strain Curve for Mild Steel", "Relations between E, G, and K", "Composite Bars in Tension", "Thermal Stress Calculations")),
                SubjectUnit(2, "Shear Force & Bending Moment", "Beams under point loads, UDL, and uniformly varying loads.", listOf("Point of Contraflexure", "SFD & BMD for Cantilever", "SFD & BMD for Simply Supported", "Relation between Load, SF, and BM")),
                SubjectUnit(3, "Bending Stresses in Beams", "Pure bending theory, section modulus, and moment of resistance.", listOf("Bending Equation M/I = σ/y = E/R", "Section Modulus for Rectangular & I-sections", "Neutral Axis Determination", "Flitched Beams")),
                SubjectUnit(4, "Shear Stresses & Deflection", "Shear stress distribution, Macaulay's method, and column buckling (Euler).", listOf("Shear Stress Formula τ = VQ/Ib", "Shear Stress in I, T, and Circular Beams", "Euler Critical Load for Columns", "Slenderness Ratio Limitations"))
            )
        ),
        Subject(
            id = "ubter_ce_501",
            board = "UBTER",
            branch = "CIVIL",
            semester = 5,
            name = "Estimating, Costing & Valuation",
            code = "CE-501",
            unitCount = 4,
            description = "Units of measurements, detailed estimates of masonry buildings, analysis of rates, and PWD contract procedures.",
            units = listOf(
                SubjectUnit(1, "Principles of Estimating", "Types of estimates, items of work, and long wall-short wall method.", listOf("Plinth Area vs Cube Rate Estimate", "Long Wall-Short Wall Method", "Center Line Method", "Units of Measurement per IS 1200")),
                SubjectUnit(2, "Detailed Estimation of Buildings", "Earthwork in excavation, foundation concrete, brickwork, and plastering.", listOf("Excavation Quantity Measurement", "Brickwork Deduction for Openings", "RCC Slab & Beam Steel Calculation", "Painting & Flooring Estimates")),
                SubjectUnit(3, "Analysis of Rates & Specifications", "Labour and material constants, overheads, contractor profit, and PWD rates.", listOf("Rate Analysis of 1:2:4 Concrete", "Rate Analysis of First Class Brickwork", "PWD Schedule of Rates (SOR)", "Task Work of Labourers")),
                SubjectUnit(4, "Valuation & Tenders", "Scrap value, salvage value, sinking fund, depreciation, and tender notices.", listOf("Depreciation Calculation Methods", "Sinking Fund Formula", "Earnest Money vs Security Deposit", "Types of PWD Contracts"))
            )
        ),
        Subject(
            id = "ubter_ce_502",
            board = "UBTER",
            branch = "CIVIL",
            semester = 5,
            name = "Environmental Engineering",
            code = "CE-502",
            unitCount = 4,
            description = "Water demand forecasting, water treatment processes, sewer design, and wastewater disposal.",
            units = listOf(
                SubjectUnit(1, "Water Demand & Sources", "Per capita demand, population forecasting methods, and intake structures.", listOf("Arithmetic vs Incremental Increase Method", "Fluctuations in Water Demand", "Groundwater vs Surface Water", "River & Canal Intakes")),
                SubjectUnit(2, "Water Treatment Processes", "Sedimentation, coagulation, filtration (rapid sand), and chlorination.", listOf("Plain vs Coagulated Sedimentation", "Alum as Coagulant Working", "Slow vs Rapid Sand Filter", "Breakpoint Chlorination Curve")),
                SubjectUnit(3, "Sewage Conveyance & Quality", "Dry weather flow, sewer pipes, manholes, and sewage characteristics.", listOf("Self-Cleansing Velocity in Sewers", "Manholes & Drop Manholes", "BOD and COD Significance", "Decomposition of Sewage")),
                SubjectUnit(4, "Sewage Treatment & Disposal", "Septic tanks, trickling filters, activated sludge process, and sludge drying.", listOf("Septic Tank Design for 50 Users", "Trickling Filter Operation", "Activated Sludge Flow Diagram", "Sludge Digestion & Drying Beds"))
            )
        )
    )

    // 16 Clearly Labeled, Reliable Resources with Real Accessible Links and Transparent Attributions
    val sampleResources: List<StudyResource> = listOf(
        // RESOURCE 1: Notes - Surveying - I
        StudyResource(
            id = "res_01",
            subjectId = "ubter_ce_301",
            subjectName = "Surveying - I",
            title = "Chain & Compass Surveying: Comprehensive Formula Sheet & Theory",
            unit = 1,
            type = ResourceType.NOTES,
            documentUrl = "https://www.ubter.in",
            source = "Faculty Notes, Govt Polytechnic Kashipur (Civil Dept)",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Aug 2024",
            semester = 3,
            description = "Complete student revision sheet covering surveying fundamental principles, tape corrections (pull, sag, temperature), whole circle bearing (WCB) to quadrantal bearing (QB) conversions, and local attraction detection steps.",
            topicsCovered = listOf("Surveying Fundamental Principles", "Pull, Temperature & Sag Corrections", "WCB vs Reduced Bearing (QB)", "Local Attraction Correction Table"),
            pageCountOrLength = "18 Pages (Summary Guide)",
            fileFormat = "PDF Study Guide"
        ),

        // RESOURCE 2: Notes - Concrete Technology
        StudyResource(
            id = "res_02",
            subjectId = "ubter_ce_302",
            subjectName = "Concrete Technology",
            title = "Concrete Mix Design (IS 10262) & Workability Principles",
            unit = 4,
            type = ResourceType.NOTES,
            documentUrl = "https://archive.org/details/concrete-technology",
            source = "PolyNotes Uttarakhand Faculty Circle",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Sep 2024",
            semester = 3,
            description = "Step-by-step numerical methodology for M20 and M25 grade mix design as per Indian Standard IS 10262, covering target mean strength, water-cement ratio determination, and moisture absorption adjustments.",
            topicsCovered = listOf("IS 10262 Step-by-Step Flowchart", "Characteristic Strength vs Target Mean Strength", "Free Water-to-Cement Ratio Chart", "Workability Slump Recommendations"),
            pageCountOrLength = "24 Pages (Handwritten & Typed)",
            fileFormat = "PDF Reference"
        ),

        // RESOURCE 3: Notes - Hydraulics
        StudyResource(
            id = "res_03",
            subjectId = "ubter_ce_303",
            subjectName = "Hydraulics & Fluid Mechanics",
            title = "Bernoulli's Theorem & Pipe Flow Energy Losses",
            unit = 2,
            type = ResourceType.NOTES,
            documentUrl = "https://archive.org/details/engineeringmecha00timouoft",
            source = "Govt Polytechnic Srinagar (Garhwal) Notes Bank",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Oct 2024",
            semester = 3,
            description = "Derivation of Bernoulli's energy theorem, practical application in Venturimeter and Orifice meter, Darcy-Weisbach friction factor calculations, and drawing hydraulic gradient line (HGL) vs total energy line (TEL).",
            topicsCovered = listOf("Bernoulli Energy Equation Derivation", "Venturimeter Coefficient of Discharge", "Major & Minor Losses in Pipes", "HGL and TEL Step-by-Step Curves"),
            pageCountOrLength = "20 Pages",
            fileFormat = "PDF Notes"
        ),

        // RESOURCE 4: Notes - Soil Mechanics
        StudyResource(
            id = "res_04",
            subjectId = "ubter_ce_402",
            subjectName = "Soil Mechanics & Foundation",
            title = "3-Phase Soil System, Atterberg Limits & Permeability",
            unit = 1,
            type = ResourceType.NOTES,
            documentUrl = "https://archive.org/details/soilmechanicsfou0000unse",
            source = "Faculty Notes, Govt Polytechnic Pitthuwala, Dehradun",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Nov 2024",
            semester = 4,
            description = "Core formulas of soil mechanics: Void ratio (e), porosity (n), degree of saturation (S), water content (w). Plasticity chart (A-line equation) for Indian Standard soil classification.",
            topicsCovered = listOf("3-Phase Diagram Basic Relations", "Casagrande Liquid Limit Device", "Plasticity Index & A-Line Formula", "Darcy's Law of Permeability"),
            pageCountOrLength = "22 Pages",
            fileFormat = "PDF Study Guide"
        ),

        // RESOURCE 5: Notes - Structural Mechanics
        StudyResource(
            id = "res_05",
            subjectId = "ubter_ce_403",
            subjectName = "Structural Mechanics (SOM)",
            title = "Shear Force (SFD) & Bending Moment Diagrams (BMD) Guide",
            unit = 2,
            type = ResourceType.NOTES,
            documentUrl = "https://archive.org/details/strengthofmateri00timouoft",
            source = "Dehradun Polytechnic Peer Study Circle",
            reviewStatus = ReviewStatus.PEER_REVIEWED,
            uploadedAt = "Dec 2024",
            semester = 4,
            description = "Solved step-by-step visual cases for simply supported, cantilever, and overhanging beams subjected to point loads and uniformly distributed loads (UDL). Locating the point of contraflexure.",
            topicsCovered = listOf("Sign Conventions for SF & BM", "Simply Supported Beam with UDL", "Cantilever Beam Reactions", "Point of Contraflexure Calculation"),
            pageCountOrLength = "16 Pages",
            fileFormat = "PDF Visual Guide"
        ),

        // RESOURCE 6: Notes - Estimating & Costing
        StudyResource(
            id = "res_06",
            subjectId = "ubter_ce_501",
            subjectName = "Estimating, Costing & Valuation",
            title = "Long Wall-Short Wall & Center Line Building Quantity Estimation",
            unit = 1,
            type = ResourceType.NOTES,
            documentUrl = "https://archive.org/details/estimatingcostin0000dutt",
            source = "Govt Polytechnic Nainital Civil Dept",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Jan 2025",
            semester = 5,
            description = "Detailed standard measurement rules for two-room diploma model residential building. Earthwork excavation, lime concrete in foundation, first class brickwork in 1:6 cement mortar, and RCC slab reinforcement.",
            topicsCovered = listOf("Long Wall - Short Wall Method", "Center Line Method for Junctions", "Standard Deductions for Doors & Windows", "Abstract of Cost & Contingency Rules"),
            pageCountOrLength = "26 Pages",
            fileFormat = "PDF Manual"
        ),

        // RESOURCE 7: Practicals - Surveying Lab
        StudyResource(
            id = "res_07",
            subjectId = "ubter_ce_301",
            subjectName = "Surveying - I",
            title = "Levelling & Contouring Field Book Practical Record",
            unit = 3,
            type = ResourceType.PRACTICALS,
            documentUrl = "https://archive.org/details/elementarysurvey00breegoog",
            source = "Surveying Lab Manual, Govt Polytechnic Dehradun",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Aug 2024",
            semester = 3,
            description = "Standard civil diploma lab file record with field readings: Profile levelling, reciprocal levelling across obstacle, determination of Reduced Levels (RL) by Collimation and Rise & Fall methods with arithmetic checks.",
            topicsCovered = listOf("Dumpy Level Temporary Adjustments", "Collimation (HI) Method Tabulation", "Rise and Fall Method Arithmetic Check", "Direct Grid Method for Contouring"),
            pageCountOrLength = "14 Pages (Lab File)",
            fileFormat = "PDF Lab Record"
        ),

        // RESOURCE 8: Practicals - Concrete Tech Lab
        StudyResource(
            id = "res_08",
            subjectId = "ubter_ce_302",
            subjectName = "Concrete Technology",
            title = "Slump Cone & Compressive Strength Cube Test Lab File",
            unit = 3,
            type = ResourceType.PRACTICALS,
            documentUrl = "https://archive.org/details/concrete-technology",
            source = "Material Testing Lab, Govt Polytechnic Roorkee",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Sep 2024",
            semester = 3,
            description = "Complete experiment report for determination of concrete workability using standard slump cone apparatus (IS 1199) and 7-day & 28-day compressive strength of 150mm concrete cubes on CTM.",
            topicsCovered = listOf("Slump Cone Apparatus Dimensions", "True Slump vs Shear vs Collapse", "150mm Cube Casting & Curing", "Compressive Testing Machine Calculations"),
            pageCountOrLength = "12 Pages",
            fileFormat = "PDF Experiment Manual"
        ),

        // RESOURCE 9: Practicals - Soil Mechanics Lab
        StudyResource(
            id = "res_09",
            subjectId = "ubter_ce_402",
            subjectName = "Soil Mechanics & Foundation",
            title = "Direct Shear Test & Standard Proctor Compaction Lab Report",
            unit = 2,
            type = ResourceType.PRACTICALS,
            documentUrl = "https://archive.org/details/soilmechanicsfou0000unse",
            source = "Geotechnical Lab, Govt Polytechnic Kashipur",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Oct 2024",
            semester = 4,
            description = "Laboratory manual detailing: (1) Standard Proctor Compaction test for finding Optimum Moisture Content (OMC) and Maximum Dry Density (MDD), and (2) Direct Shear Test for cohesion (c) and angle of internal friction (phi).",
            topicsCovered = listOf("Proctor Hammer Energy & Mould Specs", "Water Content vs Dry Density Curve", "Shear Box 60mm Setup", "Plotting Normal Stress vs Shear Stress"),
            pageCountOrLength = "15 Pages",
            fileFormat = "PDF Lab File"
        ),

        // RESOURCE 10: Practicals - Environmental Lab
        StudyResource(
            id = "res_10",
            subjectId = "ubter_ce_502",
            subjectName = "Environmental Engineering",
            title = "Water Testing Lab: pH, Turbidity, Total Hardness & Chlorides",
            unit = 3,
            type = ResourceType.PRACTICALS,
            documentUrl = "https://archive.org/details/environmentaleng0000peav",
            source = "Public Health Lab, Govt Polytechnic Haridwar",
            reviewStatus = ReviewStatus.PEER_REVIEWED,
            uploadedAt = "Nov 2024",
            semester = 5,
            description = "Standard testing procedures for potable drinking water quality as per IS 10500 standards: Nephelometric turbidity meter, EDTA titration for total hardness, and Mohr's method for chloride ion concentration.",
            topicsCovered = listOf("Nephelometer Calibration (NTU)", "EDTA Titration with Eriochrome Black-T", "Permissible Limits under IS 10500", "Residual Chlorine Testing via Chloroscope"),
            pageCountOrLength = "16 Pages",
            fileFormat = "PDF Lab Record"
        ),

        // RESOURCE 11: PYQs - Surveying - I
        StudyResource(
            id = "res_11",
            subjectId = "ubter_ce_301",
            subjectName = "Surveying - I",
            title = "Surveying - I Previous Year Board Solved Questions Bank",
            unit = 1,
            type = ResourceType.PYQS,
            documentUrl = "https://www.ubter.in",
            source = "Uttarakhand Technical Board Student Forum",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Dec 2024",
            semester = 3,
            description = "Categorized compilation of recurring 5-mark and 10-mark questions from recent UBTER winter and summer semester examinations with model answers and sketch diagrams.",
            topicsCovered = listOf("Comparison of Chain vs Compass Surveying", "Local Attraction Numerical Problem (10 Marks)", "Reciprocal Levelling Numerical (10 Marks)", "Two-Point Problem Graphic Procedure"),
            pageCountOrLength = "28 Pages",
            fileFormat = "PDF Solved Bank"
        ),

        // RESOURCE 12: PYQs - Concrete Technology
        StudyResource(
            id = "res_12",
            subjectId = "ubter_ce_302",
            subjectName = "Concrete Technology",
            title = "Concrete Technology UBTER Model Question Paper & Solutions",
            unit = 1,
            type = ResourceType.PYQS,
            documentUrl = "https://www.ubter.in",
            source = "Senior Diploma Scholars, GP Dehradun",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Jan 2025",
            semester = 3,
            description = "Comprehensive past examination paper solutions focusing on Bogues compounds, hydration kinetics, bulking of sand phenomena, and admixtures (plasticizers and superplasticizers).",
            topicsCovered = listOf("Heat of Hydration of C3S and C3A", "Fineness Modulus Numerical Analysis", "Abrams Water-Cement Ratio Law", "Causes of Bleeding and Laitance"),
            pageCountOrLength = "24 Pages",
            fileFormat = "PDF Solved Bank"
        ),

        // RESOURCE 13: PYQs - Soil Mechanics
        StudyResource(
            id = "res_13",
            subjectId = "ubter_ce_402",
            subjectName = "Soil Mechanics & Foundation",
            title = "Soil Mechanics Semester End Solved Numericals & Theory Bank",
            unit = 3,
            type = ResourceType.PYQS,
            documentUrl = "https://www.ubter.in",
            source = "Roorkee Polytechnic Alumni Group",
            reviewStatus = ReviewStatus.PEER_REVIEWED,
            uploadedAt = "Feb 2025",
            semester = 4,
            description = "Curated question set focusing on Terzaghi's ultimate bearing capacity equation for strip, square, and circular footings, and phase diagram numericals with step-by-step marking schemes.",
            topicsCovered = listOf("Terzaghi General Shear Failure Equation", "Void Ratio and Water Content Numerical", "Unconfined Compression Strength of Clay", "Plate Load Test Corrections"),
            pageCountOrLength = "20 Pages",
            fileFormat = "PDF Solved Papers"
        ),

        // RESOURCE 14: PYQs - Estimating & Costing
        StudyResource(
            id = "res_14",
            subjectId = "ubter_ce_501",
            subjectName = "Estimating, Costing & Valuation",
            title = "Estimating & Costing Board Examination Model Solution Set",
            unit = 2,
            type = ResourceType.PYQS,
            documentUrl = "https://www.ubter.in",
            source = "PolyNotes State Faculty Contributor Group",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Feb 2025",
            semester = 5,
            description = "Model solved questions covering rate analysis of M15 concrete per cubic meter, first class brickwork in foundation, PWD tender notification format, and sinking fund numerical.",
            topicsCovered = listOf("Rate Analysis of 10 m³ Brickwork", "Sinking Fund Formula & Numerical", "Types of Contracts: Item Rate vs Lump Sum", "Earnest Money Deposit (EMD) Calculation"),
            pageCountOrLength = "22 Pages",
            fileFormat = "PDF Solved Bank"
        ),

        // RESOURCE 15: Viva - Surveying
        StudyResource(
            id = "res_15",
            subjectId = "ubter_ce_301",
            subjectName = "Surveying - I",
            title = "50 High-Yield Surveying & Levelling External Viva Questions",
            unit = 1,
            type = ResourceType.VIVA,
            documentUrl = "https://archive.org/details/elementarysurvey00breegoog",
            source = "External Examiner Prep Sheet, GP Dehradun",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Mar 2025",
            semester = 3,
            description = "Rapid question-and-answer guide commonly asked by external polytechnic examiners during practical examinations: Line of collimation, bench mark, foresight, back sight, and local attraction causes.",
            topicsCovered = listOf("Difference between Fore Sight & Back Sight", "What is an Inverted Staff Reading?", "Why is Prism Used in Prismatic Compass?", "What is Sensitivity of Bubble Tube?"),
            pageCountOrLength = "10 Pages (Flashcard Format)",
            fileFormat = "PDF Viva Prep"
        ),

        // RESOURCE 16: Viva - Concrete Tech
        StudyResource(
            id = "res_16",
            subjectId = "ubter_ce_302",
            subjectName = "Concrete Technology",
            title = "Concrete & Materials Practical Rapid-Fire Viva Prep",
            unit = 2,
            type = ResourceType.VIVA,
            documentUrl = "https://archive.org/details/concrete-technology",
            source = "Faculty Interview Panel, GP Roorkee",
            reviewStatus = ReviewStatus.FACULTY_VERIFIED,
            uploadedAt = "Mar 2025",
            semester = 3,
            description = "Direct, succinct answers to viva questions on cement testing, slump values for slabs and columns, curing duration in Uttarakhand climate, initial and final setting time standards.",
            topicsCovered = listOf("Standard Consistency of Cement (P)", "Initial & Final Setting Time Limits", "Why do We Add Gypsum to Clinker?", "What is Characteristic 28-day Strength?"),
            pageCountOrLength = "12 Pages (Q&A Format)",
            fileFormat = "PDF Viva Prep"
        )
    )
}
