package com.digitaldiscipline.spike.intervention.catalog

import com.digitaldiscipline.spike.intervention.model.InterventionCategory
import com.digitaldiscipline.spike.intervention.model.InterventionDefinition
import com.digitaldiscipline.spike.intervention.model.ValidationType

object InterventionCatalog {

    private val catalog: Map<String, InterventionDefinition> = listOf(
        // =========================================================================
        // A. MOVEMENT (10)
        // =========================================================================
        InterventionDefinition(
            id = "PUSH_UPS",
            title = "Push-ups",
            description = "Build upper-body strength and clear your mind with a set of push-ups.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "💪",
            calmPrompt = "Take a breath and do your push-ups.",
            instructions = "Place the phone on the floor beneath your chest. Lower down until your chest approaches the screen.",
            defaultReps = 10,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "SQUATS",
            title = "Bodyweight Squats",
            description = "Activate leg muscles and boost circulation with deep squats.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "🏋️",
            calmPrompt = "Let's reset with a quick set of squats.",
            instructions = "Hold your phone securely in front of your chest and perform steady, deep squats.",
            defaultReps = 10,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "LUNGES",
            title = "Alternating Lunges",
            description = "Strengthen your legs and test balance with steady lunges.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "🦵",
            calmPrompt = "Step forward with focus.",
            instructions = "Hold your phone and step forward into a lunge, alternating legs with each rep.",
            defaultReps = 10,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "PLANK",
            title = "Core Plank Hold",
            description = "Engage your entire core with a focused isometric plank hold.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🧘",
            calmPrompt = "Hold steady and breathe.",
            instructions = "Rest on your forearms with core engaged and spine neutral.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "WALL_SIT",
            title = "Wall Sit",
            description = "Hold a seated position against a wall to build leg endurance.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🧱",
            calmPrompt = "Settle against the wall and hold.",
            instructions = "Back against the wall, thighs parallel to the ground.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "JUMPING_JACKS",
            title = "Jumping Jacks",
            description = "Elevate heart rate and shake off lethargy.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "⭐",
            calmPrompt = "Jump with rhythm and lightness.",
            instructions = "Hold phone in hand or pocket and perform jumping jacks.",
            defaultReps = 15,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "HIGH_KNEES",
            title = "High Knees",
            description = "Drive knees upward to activate core and cardio.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "🏃",
            calmPrompt = "Lift your knees high.",
            instructions = "Perform high knees in place at a steady tempo.",
            defaultReps = 20,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "CALF_RAISES",
            title = "Calf Raises",
            description = "Lift up onto your toes to strengthen calves and ankles.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "🦶",
            calmPrompt = "Rise smoothly onto your toes.",
            instructions = "Stand straight and raise your heels off the ground smoothly.",
            defaultReps = 15,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "SIT_TO_STAND",
            title = "Sit-to-Stand",
            description = "Stand up from your chair and sit down with control.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "🪑",
            calmPrompt = "Stand up with control.",
            instructions = "From a seated position, stand fully upright and sit back down without using hands.",
            defaultReps = 10,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "STRETCH",
            title = "Full Body Stretch",
            description = "Release tension across your neck, back, and shoulders.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🙆",
            calmPrompt = "Reach tall and release tension.",
            instructions = "Reach your arms overhead, gently bend side to side, and roll your shoulders.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),

        // =========================================================================
        // B. UPPER BODY (1)
        // =========================================================================
        InterventionDefinition(
            id = "PULL_UPS",
            title = "Pull-ups",
            description = "Complete a set of pull-ups on a bar.",
            category = InterventionCategory.UPPER_BODY,
            validationType = ValidationType.MANUAL_CONFIRMATION,
            iconEmoji = "🧗",
            calmPrompt = "Step up to the bar.",
            instructions = "Perform your set on a pull-up bar, then confirm completion.",
            defaultReps = 5,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),

        // =========================================================================
        // C. BREATHING (4)
        // =========================================================================
        InterventionDefinition(
            id = "BOX_BREATHING",
            title = "Box Breathing (4-4-4-4)",
            description = "Regulate your autonomic nervous system with 4-second box cycles.",
            category = InterventionCategory.BREATHING,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🌬️",
            calmPrompt = "Inhale, hold, exhale, hold.",
            instructions = "Follow the pacing circle: Inhale 4s, Hold 4s, Exhale 4s, Hold 4s.",
            defaultReps = 0,
            defaultDurationSeconds = 32,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "FOUR_TWO_SIX_BREATHING",
            title = "4-2-6 Calming Breath",
            description = "Prolonged exhale to trigger the parasympathetic relaxation response.",
            category = InterventionCategory.BREATHING,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🌊",
            calmPrompt = "Slow down and let your exhale linger.",
            instructions = "Inhale for 4s, hold for 2s, and exhale smoothly for 6s.",
            defaultReps = 0,
            defaultDurationSeconds = 36,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "ONE_MINUTE_BREATHING_RESET",
            title = "1-Minute Breathing Reset",
            description = "A sixty-second conscious breathing pause.",
            category = InterventionCategory.BREATHING,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🍃",
            calmPrompt = "Take one minute for deep, gentle breaths.",
            instructions = "Breathe in through your nose and out through your mouth at a natural pace.",
            defaultReps = 0,
            defaultDurationSeconds = 60,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "THREE_BREATH_RESET",
            title = "30s Deep Breath Reset",
            description = "Deep, grounding breaths before you open your device.",
            category = InterventionCategory.BREATHING,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "✨",
            calmPrompt = "Slow breaths to center yourself.",
            instructions = "Take slow, deep abdominal breaths for 30 seconds.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),

        // =========================================================================
        // D. MEDITATION (3)
        // =========================================================================
        InterventionDefinition(
            id = "THIRTY_SECOND_MEDITATION",
            title = "30-Second Meditation",
            description = "Close your eyes and bring total stillness to your attention.",
            category = InterventionCategory.MEDITATION,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🪷",
            calmPrompt = "Be still for 30 seconds.",
            instructions = "Rest your phone, close your eyes, and notice the present moment.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "ONE_MINUTE_MEDITATION",
            title = "1-Minute Meditation",
            description = "A one-minute silent pause to disconnect from digital noise.",
            category = InterventionCategory.MEDITATION,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🕯️",
            calmPrompt = "One quiet minute of awareness.",
            instructions = "Sit comfortably, release muscle tension, and let thoughts pass.",
            defaultReps = 0,
            defaultDurationSeconds = 60,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "MINDFUL_PAUSE",
            title = "30s Mindful Pause",
            description = "A conscious 30-second pause to interrupt impulsive browsing.",
            category = InterventionCategory.MEDITATION,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "⚡",
            calmPrompt = "Pause and ask: 'Do I really need this right now?'",
            instructions = "Observe the impulse without acting on it immediately.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),

        // =========================================================================
        // E. YOGA / MOBILITY (6)
        // =========================================================================
        InterventionDefinition(
            id = "MOUNTAIN_POSE",
            title = "Mountain Pose (Tadasana)",
            description = "Stand tall with feet grounded and spine aligned.",
            category = InterventionCategory.YOGA_MOBILITY,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🏔️",
            calmPrompt = "Stand tall, steady, and grounded.",
            instructions = "Ground your feet evenly, lengthen your spine, and relax your shoulders.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "FORWARD_FOLD",
            title = "Standing Forward Fold",
            description = "Hinge forward from the hips to release hamstrings and lower back.",
            category = InterventionCategory.YOGA_MOBILITY,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🙇",
            calmPrompt = "Fold forward and let your head hang heavy.",
            instructions = "Bend your knees slightly, hinge at your hips, and let gravity lengthen your back.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "TREE_POSE",
            title = "Tree Pose (Vrksasana)",
            description = "Find balance on one leg with palms joined in front of your chest.",
            category = InterventionCategory.YOGA_MOBILITY,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🌳",
            calmPrompt = "Find your balance and hold.",
            instructions = "Place one foot against your calf or thigh and focus on a steady point.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "CHILD_POSE",
            title = "Child's Pose (Balasana)",
            description = "Rest your hips on your heels and extend your arms forward.",
            category = InterventionCategory.YOGA_MOBILITY,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🕊️",
            calmPrompt = "Rest and release all effort.",
            instructions = "Kneel on the floor, fold your torso over your thighs, and rest your forehead down.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "SHOULDER_STRETCH",
            title = "Cross-Body Shoulder Stretch",
            description = "Pull one arm across your chest to release shoulder tightness.",
            category = InterventionCategory.YOGA_MOBILITY,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "🎗️",
            calmPrompt = "Gently open the back of your shoulders.",
            instructions = "Draw your arm across your chest with opposite forearm. Hold each side.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "MINI_SUN_SALUTATION",
            title = "Mini Sun Salutation",
            description = "Inhale arms up, exhale fold forward, inhale flat back, exhale stand tall.",
            category = InterventionCategory.YOGA_MOBILITY,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "☀️",
            calmPrompt = "Flow through breath and movement.",
            instructions = "Move smoothly through one complete breath-movement sequence.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),

        // =========================================================================
        // F. SIMPLE PHYSICAL RESET (5)
        // =========================================================================
        InterventionDefinition(
            id = "STAND_UP",
            title = "Stand Up & Shake Off",
            description = "Stand up and shake out body tension in front of the camera AI.",
            category = InterventionCategory.MOVEMENT,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "🧍",
            calmPrompt = "Stand up and shake out tension.",
            instructions = "Step back so your body is visible in the camera frame. Stand tall and shake out your arms, hands, and legs.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "WALK_30_STEPS",
            title = "Walk 30 Steps",
            description = "Walk around your room or hallway to get blood moving.",
            category = InterventionCategory.PHYSICAL_RESET,
            validationType = ValidationType.SENSOR_VALIDATED,
            iconEmoji = "🚶",
            calmPrompt = "Take 30 steps away from your desk.",
            instructions = "Walk with your phone in hand until 30 steps are detected.",
            defaultReps = 30,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "DRINK_WATER",
            title = "Drink a Glass of Water",
            description = "Hydrate before engaging with your screen.",
            category = InterventionCategory.PHYSICAL_RESET,
            validationType = ValidationType.MANUAL_CONFIRMATION,
            iconEmoji = "💧",
            calmPrompt = "Hydrate your body.",
            instructions = "Drink a full glass of water, then tap to continue.",
            defaultReps = 0,
            defaultDurationSeconds = 0,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "LOOK_AWAY_FROM_SCREEN",
            title = "30s Eye Relief",
            description = "Look at an object 20 feet away for 30 seconds to prevent eye fatigue.",
            category = InterventionCategory.PHYSICAL_RESET,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "👀",
            calmPrompt = "Look out a window or into the distance.",
            instructions = "Focus your gaze on a distant point to relax your eye muscles.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "POSTURE_RESET",
            title = "Posture Alignment",
            description = "Pull shoulders down, align neck over spine, and uncross legs.",
            category = InterventionCategory.PHYSICAL_RESET,
            validationType = ValidationType.TIMER_VALIDATED,
            iconEmoji = "📐",
            calmPrompt = "Straighten your spine.",
            instructions = "Roll shoulders back, tuck your chin slightly, and sit or stand tall.",
            defaultReps = 0,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),

        // =========================================================================
        // G. COGNITIVE MICRO-CHALLENGES (6)
        // =========================================================================
        InterventionDefinition(
            id = "IMAGE_PUZZLE_3X3",
            title = "9-Piece Image Puzzle",
            description = "Reassemble the jumbled 3x3 image puzzle within 30 seconds to awaken your focus.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🧩",
            calmPrompt = "Reassemble the 9-piece image puzzle in 30s.",
            instructions = "Tap two pieces to swap them. Match numbers 1 through 9 before the 30s timer runs out.",
            defaultReps = 1,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "HANGMAN_CLASSIC",
            title = "Mindful Hangman Word Guess",
            description = "Decrypt the mystery mindfulness or focus word before 6 strikes to ground your thinking.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🔤",
            calmPrompt = "Guess the mindful word before 6 strikes.",
            instructions = "Tap letters to guess the hidden word. Max 6 mistakes allowed before timer resets.",
            defaultReps = 1,
            defaultDurationSeconds = 45,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "SIMPLE_MATH",
            title = "30s Mental Math Sprint",
            description = "Solve arithmetic equations over 30 seconds to awaken your prefrontal cortex.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🧮",
            calmPrompt = "Engage your prefrontal cortex for 30 seconds.",
            instructions = "Solve progressive arithmetic equations over 30 seconds.",
            defaultReps = 5,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "MEMORY_SEQUENCE",
            title = "30s Memory Sequence",
            description = "Remember and repeat progressive multi-tile sequences.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🧠",
            calmPrompt = "Watch the pattern, then repeat it.",
            instructions = "Memorize the highlighted tile order and tap in sequence.",
            defaultReps = 5,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "TAP_SEQUENCE",
            title = "30s Focus Tap Sequence",
            description = "Tap numbers in ascending order under timed focus.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🔢",
            calmPrompt = "Tap numbers in sequence as they appear.",
            instructions = "Tap numbers in ascending order across 3 progressive rounds.",
            defaultReps = 5,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "REACTION_TEST",
            title = "30s Reaction Control Test",
            description = "Test impulse inhibitory control across 3 timed rounds.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🎯",
            calmPrompt = "Wait patiently on red, tap instantly on green.",
            instructions = "Hold impulse on red. Tap the moment green appears across 3 rounds.",
            defaultReps = 3,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "PATTERN_MATCH",
            title = "30s Visual Pattern Match",
            description = "Find matching visual symbols across progressive stages.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🧩",
            calmPrompt = "Select the matching shape.",
            instructions = "Identify matching patterns over a 30-second cognitive focus interval.",
            defaultReps = 3,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "QUICK_RECALL",
            title = "30s Working Word Recall",
            description = "Memorize word sequences and recall missing terms under timed delay.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "📝",
            calmPrompt = "Memorize and recall.",
            instructions = "Memorize word list during pause, then identify the missing word.",
            defaultReps = 3,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "STROOP_TEST",
            title = "30s Stroop Conflict Sprint",
            description = "Test executive inhibitory control by naming font ink colors under timed pressure.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🧠",
            calmPrompt = "Name the ink color under 30s timed pressure.",
            instructions = "Tap the button matching the font color, not the written word.",
            defaultReps = 6,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "MATH_SPRINT",
            title = "30s Mental Math Sprint",
            description = "30 seconds of multi-step arithmetic challenges to awaken executive control.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "⚡",
            calmPrompt = "Calculate quickly to stimulate executive control.",
            instructions = "Solve progressive math problems during the 30-second sprint.",
            defaultReps = 5,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "MEMORY_MATRIX",
            title = "30s Working Memory Matrix",
            description = "3 progressive levels of spatial memory recall across 30 seconds.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🧩",
            calmPrompt = "Watch the sequence and tap in exact order.",
            instructions = "Recall progressive 3x3 and 4x4 spatial patterns across 3 levels.",
            defaultReps = 5,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "MINDFUL_READING",
            title = "30s Mindful Wisdom Reflection",
            description = "25-second Stoic wisdom reflection pause followed by comprehension verification.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "📖",
            calmPrompt = "Read carefully and reflect for 25 seconds.",
            instructions = "Take a 25-second reading pause, then answer the reflection check.",
            defaultReps = 1,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "INTENTIONAL_WRITING",
            title = "30s Purpose Journal",
            description = "A 30-second cool-down to declare your specific intention and next offline action.",
            category = InterventionCategory.COGNITIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "✍️",
            calmPrompt = "What specific task are you seeking to do?",
            instructions = "Complete the 30s reflection declaring your intention and next offline task.",
            defaultReps = 1,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),

        // =========================================================================
        // H. CREATIVE PRODUCER & NEURO-BEHAVIORAL RESETS (8)
        // =========================================================================
        InterventionDefinition(
            id = "ZEN_ENSO_CANVAS",
            title = "Zen Canvas: 1-Stroke Enso",
            description = "Draw a smooth single-stroke Enso circle or creative micro-doodle to activate motor planning and kill passive scrolling trance.",
            category = InterventionCategory.CREATIVE_FLOW,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🎨",
            calmPrompt = "Draw a single continuous stroke.",
            instructions = "Draw a smooth, continuous single-stroke circle without lifting your finger.",
            defaultReps = 1,
            defaultDurationSeconds = 20,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "SCAVENGER_HUNT",
            title = "Real-World Scavenger Hunt",
            description = "Break screen myopia by pointing camera at physical objects in your room (e.g. green plant, book, ceramic mug).",
            category = InterventionCategory.PHYSICAL_RESET,
            validationType = ValidationType.CAMERA_VALIDATED,
            iconEmoji = "📸",
            calmPrompt = "Find something in your real-world room.",
            instructions = "Point your camera to detect the requested physical item before unlocking.",
            defaultReps = 1,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "HAND_MUDRA_DEXTERITY",
            title = "Hand Mudra & Anti-Tech Dexterity",
            description = "Rhythmic bilateral finger-tap sequence and 15s Gyan Mudra hold to release doomscrolling thumb tension.",
            category = InterventionCategory.CREATIVE_FLOW,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "✋",
            calmPrompt = "Release thumb claw tension with rhythmic tapping.",
            instructions = "Follow the rhythm tap pattern (Thumb → Index → Middle → Ring → Pinky) then hold mindful mudra.",
            defaultReps = 5,
            defaultDurationSeconds = 25,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "DIVERGENT_THINKING",
            title = "Lateral Thinking Sprint",
            description = "Name 3 creative, non-obvious alternative uses for an everyday object to stimulate prefrontal cognitive novelty.",
            category = InterventionCategory.CREATIVE_FLOW,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "💡",
            calmPrompt = "Think differently. Name 3 unusual uses.",
            instructions = "Type 3 creative, non-obvious uses for the displayed everyday object.",
            defaultReps = 3,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "HAIKU_CRAFTER",
            title = "5-7-5 Haiku / 6-Word Story",
            description = "Compose a structured 3-line mindful Haiku with live syllable counter or craft a punchy 6-word story.",
            category = InterventionCategory.CREATIVE_FLOW,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "📜",
            calmPrompt = "Craft a 3-line mindful Haiku.",
            instructions = "Write Line 1 (5 syllables), Line 2 (7 syllables), Line 3 (5 syllables).",
            defaultReps = 3,
            defaultDurationSeconds = 40,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "BINAURAL_SOUNDSCAPE",
            title = "Binaural Soundscape Synthesizer",
            description = "Interactive 4-track mixer generating 40Hz Gamma focus binaural beats, gentle rain, Tibetan bowls, and lo-fi chords.",
            category = InterventionCategory.MINDFUL_PERSPECTIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🎹",
            calmPrompt = "Mix a 20-second calming binaural focus loop.",
            instructions = "Adjust the 4 audio channels to create your focus soundscape.",
            defaultReps = 0,
            defaultDurationSeconds = 20,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "FUTURE_SELF_CAPSULE",
            title = "Capsule to Future Self",
            description = "Write 1 sentence your 10:00 PM self will thank you for. Delivered as a notification tonight at bedtime.",
            category = InterventionCategory.MINDFUL_PERSPECTIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "💌",
            calmPrompt = "What will your 10:00 PM self thank you for?",
            instructions = "Write a one-sentence message that will be delivered to you tonight at 10 PM.",
            defaultReps = 1,
            defaultDurationSeconds = 30,
            rewardSeconds = 600
        ),
        InterventionDefinition(
            id = "STOIC_TAROT_DECIDER",
            title = "Perspective Shift / Stoic Tarot",
            description = "3D interactive card flip with Memento Mori, Cosmic Zoom, or Inversion philosophical reframing.",
            category = InterventionCategory.MINDFUL_PERSPECTIVE,
            validationType = ValidationType.INTERACTION_VALIDATED,
            iconEmoji = "🔮",
            calmPrompt = "Flip a card for philosophical reframing.",
            instructions = "Select and flip a card, then reflect for 15 seconds.",
            defaultReps = 1,
            defaultDurationSeconds = 20,
            rewardSeconds = 600
        )
    ).associateBy { it.id }

    fun getAllInterventions(): List<InterventionDefinition> = catalog.values.toList()

    fun getIntervention(id: String): InterventionDefinition? = catalog[id]

    fun getByCategory(category: InterventionCategory): List<InterventionDefinition> =
        catalog.values.filter { it.category == category }

    fun getByValidationType(validationType: ValidationType): List<InterventionDefinition> =
        catalog.values.filter { it.validationType == validationType }

    fun getDefaultIntervention(): InterventionDefinition =
        catalog["SQUATS"] ?: catalog.values.first()
}
