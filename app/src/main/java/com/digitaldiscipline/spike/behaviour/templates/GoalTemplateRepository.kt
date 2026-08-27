package com.digitaldiscipline.spike.behaviour.templates

import com.digitaldiscipline.spike.data.local.entities.BehaviourCategory
import com.digitaldiscipline.spike.data.local.entities.BehaviourType
import com.digitaldiscipline.spike.data.local.entities.GoalCategory
import com.digitaldiscipline.spike.data.local.entities.ReplacementBehaviourEntity
import com.digitaldiscipline.spike.data.local.entities.TriggerCategory

object GoalTemplateRepository {

    // Common Replacement Behaviours
    val BEH_SQUATS_10 = ReplacementBehaviourEntity(
        behaviourId = "beh_squats_10",
        category = BehaviourCategory.PHYSICAL.name,
        type = BehaviourType.SQUATS.name,
        title = "10 Bodyweight Squats",
        description = "Move your body to reset dopamine before screen access",
        targetCount = 10,
        durationSeconds = 60,
        unit = "reps"
    )

    val BEH_PUSHUPS_10 = ReplacementBehaviourEntity(
        behaviourId = "beh_pushups_10",
        category = BehaviourCategory.PHYSICAL.name,
        type = BehaviourType.PUSHUPS.name,
        title = "10 Pushups",
        description = "Quick physical challenge to shift state",
        targetCount = 10,
        durationSeconds = 60,
        unit = "reps"
    )

    val BEH_PAUSE_10S = ReplacementBehaviourEntity(
        behaviourId = "beh_pause_10s",
        category = BehaviourCategory.MINDFUL.name,
        type = BehaviourType.MINDFUL_PAUSE.name,
        title = "10s Mindful Pause",
        description = "Take 10 seconds to notice your urge before opening",
        targetCount = 1,
        durationSeconds = 10,
        unit = "seconds"
    )

    val BEH_BREATHING_30S = ReplacementBehaviourEntity(
        behaviourId = "beh_breathing_30s",
        category = BehaviourCategory.MINDFUL.name,
        type = BehaviourType.BOX_BREATHING.name,
        title = "30s Box Breathing",
        description = "4s inhale • 4s hold • 4s exhale • 4s hold nervous system reset",
        targetCount = 1,
        durationSeconds = 30,
        unit = "seconds"
    )

    val BEH_BREATHING_60S = ReplacementBehaviourEntity(
        behaviourId = "beh_breathing_60s",
        category = BehaviourCategory.MINDFUL.name,
        type = BehaviourType.BOX_BREATHING.name,
        title = "60s Box Breathing",
        description = "Extended deep breathing cycle for calm clarity",
        targetCount = 1,
        durationSeconds = 60,
        unit = "seconds"
    )

    val BEH_STUDY_BLOCK_5M = ReplacementBehaviourEntity(
        behaviourId = "beh_study_block_5m",
        category = BehaviourCategory.STUDY.name,
        type = BehaviourType.STUDY_TIMER.name,
        title = "5-Minute Study Block",
        description = "Complete 5 focused minutes of study before screen access",
        targetCount = 5,
        durationSeconds = 300,
        unit = "minutes"
    )

    val BEH_STUDY_BLOCK_10M = ReplacementBehaviourEntity(
        behaviourId = "beh_study_block_10m",
        category = BehaviourCategory.STUDY.name,
        type = BehaviourType.STUDY_TIMER.name,
        title = "10-Minute Study Block",
        description = "Complete 10 focused minutes of study before screen access",
        targetCount = 10,
        durationSeconds = 600,
        unit = "minutes"
    )

    val BEH_TASK_BLOCK_5M = ReplacementBehaviourEntity(
        behaviourId = "beh_task_block_5m",
        category = BehaviourCategory.PRODUCTIVITY.name,
        type = BehaviourType.COMPLETE_TASK.name,
        title = "5-Minute Task Sprint",
        description = "Progress on an important task before opening distractions",
        targetCount = 5,
        durationSeconds = 300,
        unit = "minutes"
    )

    val BEH_TASK_BLOCK_10M = ReplacementBehaviourEntity(
        behaviourId = "beh_task_block_10m",
        category = BehaviourCategory.PRODUCTIVITY.name,
        type = BehaviourType.COMPLETE_TASK.name,
        title = "10-Minute Task Sprint",
        description = "Clear one priority action before distraction",
        targetCount = 10,
        durationSeconds = 600,
        unit = "minutes"
    )

    val BEH_READING_5M = ReplacementBehaviourEntity(
        behaviourId = "beh_reading_5m",
        category = BehaviourCategory.STUDY.name,
        type = BehaviourType.READ_PAGES.name,
        title = "5-Minute Reading",
        description = "Read 5 minutes of your book before screen time",
        targetCount = 5,
        durationSeconds = 300,
        unit = "minutes"
    )

    val BEH_READING_10M = ReplacementBehaviourEntity(
        behaviourId = "beh_reading_10m",
        category = BehaviourCategory.STUDY.name,
        type = BehaviourType.READ_PAGES.name,
        title = "10-Minute Reading",
        description = "Read 10 minutes of your book before screen time",
        targetCount = 10,
        durationSeconds = 600,
        unit = "minutes"
    )

    val BEH_SLEEP_WINDDOWN = ReplacementBehaviourEntity(
        behaviourId = "beh_sleep_winddown",
        category = BehaviourCategory.MINDFUL.name,
        type = BehaviourType.BOX_BREATHING.name,
        title = "Calm Wind-Down Breathing",
        description = "Relaxing breathing to prepare for restful sleep",
        targetCount = 1,
        durationSeconds = 30,
        unit = "seconds"
    )

    // Common Distraction App Catalog (India & Global Curated)
    val COMMON_DISTRACTIONS = listOf(
        // Social Media & Short Video
        DistractionAppRecommendation("com.instagram.android", "Instagram", "📸", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.google.android.youtube", "YouTube", "▶️", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.snapchat.android", "Snapchat", "👻", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.whatsapp", "WhatsApp", "💬", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("org.telegram.messenger", "Telegram", "✈️", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.twitter.android", "X / Twitter", "🐦", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.facebook.katana", "Facebook", "👤", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.reddit.frontpage", "Reddit", "🤖", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.instagram.barcelona", "Threads", "🧵", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("in.mohalla.sharechat", "ShareChat", "🇮🇳", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("in.mohalla.video", "Moj", "🎥", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.eterno.shortvideos", "Josh", "✨", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.pinterest", "Pinterest", "📌", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.discord", "Discord", "🎮", TriggerCategory.SOCIAL_MEDIA),
        DistractionAppRecommendation("com.zhiliaoapp.musically", "TikTok", "🎵", TriggerCategory.SOCIAL_MEDIA),

        // Gaming (India & Global)
        DistractionAppRecommendation("com.pubg.imobile", "BGMI (Battlegrounds)", "🔫", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.dts.freefireth", "Free Fire", "🔥", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.dts.freefiremax", "Free Fire MAX", "🔥", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.ludo.king", "Ludo King", "🎲", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.king.candycrushsaga", "Candy Crush Saga", "🍬", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.kiloo.subwaysurf", "Subway Surfers", "🏃", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.roblox.client", "Roblox", "🧱", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.activision.callofduty.shooter", "Call of Duty Mobile", "🎖️", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.supercell.clashofclans", "Clash of Clans", "⚔️", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.supercell.clashroyale", "Clash Royale", "👑", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.mojang.minecraftpe", "Minecraft", "⛏️", TriggerCategory.GAMING),
        DistractionAppRecommendation("com.gameloft.android.ANMP.GloftA9HM", "Asphalt 9 Legends", "🏎️", TriggerCategory.GAMING),

        // Video Streaming & OTT (India & Global)
        DistractionAppRecommendation("in.startv.hotstar", "JioHotstar / Disney+", "⭐", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.jio.media.ondemand", "JioCinema", "🎬", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.netflix.mediaclient", "Netflix", "🍿", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.amazon.avod.thirdpartyclient", "Prime Video", "📺", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.graymatrix.did", "Zee5", "📺", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.sonyliv", "SonyLIV", "🏆", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.mxtech.videoplayer.ad", "MX Player", "🎥", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("tv.twitch.android.app", "Twitch", "🟣", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.google.android.apps.youtube.music", "YouTube Music", "🎵", TriggerCategory.VIDEO_STREAMING),
        DistractionAppRecommendation("com.spotify.music", "Spotify", "🎧", TriggerCategory.VIDEO_STREAMING),

        // Shopping & Impulse Browsing
        DistractionAppRecommendation("com.amazon.mShop.android.shopping", "Amazon", "📦", TriggerCategory.SHOPPING),
        DistractionAppRecommendation("com.flipkart.android", "Flipkart", "🛍️", TriggerCategory.SHOPPING),
        DistractionAppRecommendation("com.myntra.android", "Myntra", "👗", TriggerCategory.SHOPPING),
        DistractionAppRecommendation("com.meesho.supply", "Meesho", "🏷️", TriggerCategory.SHOPPING),
        DistractionAppRecommendation("com.ril.ajio", "Ajio", "👕", TriggerCategory.SHOPPING),
        DistractionAppRecommendation("com.fsn.nykaa", "Nykaa", "💄", TriggerCategory.SHOPPING),

        // Food & Quick Commerce
        DistractionAppRecommendation("com.application.zomato", "Zomato", "🍔", TriggerCategory.FOOD_DELIVERY),
        DistractionAppRecommendation("in.swiggy.android", "Swiggy", "🍕", TriggerCategory.FOOD_DELIVERY),
        DistractionAppRecommendation("com.zeptoconsumerapp", "Zepto", "⚡", TriggerCategory.FOOD_DELIVERY),
        DistractionAppRecommendation("com.grofers.customerapp", "Blinkit", "🛒", TriggerCategory.FOOD_DELIVERY),

        // Web Browsers
        DistractionAppRecommendation("com.android.chrome", "Google Chrome", "🌐", TriggerCategory.CUSTOM)
    )

    // Templates
    private val templates = listOf(
        GoalTemplate(
            templateId = "tmpl_fitness",
            category = GoalCategory.FITNESS,
            name = "Get Fitter",
            shortDescription = "Do a small positive movement before opening a distracting app",
            icon = "💪",
            defaultUnit = "actions",
            defaultDailyTarget = 5,
            defaultWeeklyTarget = 25,
            recommendedReplacementBehaviours = listOf(
                BEH_SQUATS_10,
                BEH_PUSHUPS_10,
                BEH_BREATHING_30S,
                BEH_PAUSE_10S
            ),
            recommendedTriggerCategories = listOf(
                TriggerCategory.SOCIAL_MEDIA,
                TriggerCategory.VIDEO_STREAMING,
                TriggerCategory.GAMING
            ),
            defaultRewardPreset = RewardPreset.STANDARD,
            defaultDailyEarnCapSeconds = 1800,
            defaultWalletCapSeconds = 3600,
            defaultSessionCapSeconds = 900,
            onboardingCopy = "Build daily physical movement by converting distraction urges into bodyweight squats or pushups."
        ),
        GoalTemplate(
            templateId = "tmpl_study",
            category = GoalCategory.STUDY,
            name = "Study More Consistently",
            shortDescription = "Complete a focused study block before screen access",
            icon = "📚",
            defaultUnit = "blocks",
            defaultDailyTarget = 4,
            defaultWeeklyTarget = 20,
            recommendedReplacementBehaviours = listOf(
                BEH_STUDY_BLOCK_5M,
                BEH_STUDY_BLOCK_10M,
                BEH_PAUSE_10S
            ),
            recommendedTriggerCategories = listOf(
                TriggerCategory.SOCIAL_MEDIA,
                TriggerCategory.VIDEO_STREAMING,
                TriggerCategory.GAMING
            ),
            defaultRewardPreset = RewardPreset.STANDARD,
            defaultDailyEarnCapSeconds = 1800,
            defaultWalletCapSeconds = 3600,
            defaultSessionCapSeconds = 900,
            onboardingCopy = "Anchor your study habit by unlocking short screen breaks after completing dedicated study blocks."
        ),
        GoalTemplate(
            templateId = "tmpl_productivity",
            category = GoalCategory.PRODUCTIVITY,
            name = "Be More Productive",
            shortDescription = "Get important tasks done before distraction",
            icon = "💼",
            defaultUnit = "tasks",
            defaultDailyTarget = 4,
            defaultWeeklyTarget = 20,
            recommendedReplacementBehaviours = listOf(
                BEH_TASK_BLOCK_5M,
                BEH_TASK_BLOCK_10M,
                BEH_PAUSE_10S
            ),
            recommendedTriggerCategories = listOf(
                TriggerCategory.SOCIAL_MEDIA,
                TriggerCategory.VIDEO_STREAMING,
                TriggerCategory.GAMING
            ),
            defaultRewardPreset = RewardPreset.STANDARD,
            defaultDailyEarnCapSeconds = 1800,
            defaultWalletCapSeconds = 3600,
            defaultSessionCapSeconds = 900,
            onboardingCopy = "Train the habit of clearing one priority task before letting yourself open entertainment apps."
        ),
        GoalTemplate(
            templateId = "tmpl_mindfulness",
            category = GoalCategory.MINDFULNESS,
            name = "Be More Mindful",
            shortDescription = "Pause and breathe before entering high-distraction apps",
            icon = "🧘",
            defaultUnit = "pauses",
            defaultDailyTarget = 5,
            defaultWeeklyTarget = 25,
            recommendedReplacementBehaviours = listOf(
                BEH_BREATHING_30S,
                BEH_PAUSE_10S,
                BEH_BREATHING_60S
            ),
            recommendedTriggerCategories = listOf(
                TriggerCategory.SOCIAL_MEDIA,
                TriggerCategory.VIDEO_STREAMING,
                TriggerCategory.GAMING
            ),
            defaultRewardPreset = RewardPreset.LIGHT,
            defaultDailyEarnCapSeconds = 1200,
            defaultWalletCapSeconds = 2400,
            defaultSessionCapSeconds = 600,
            onboardingCopy = "Develop calm awareness by taking conscious breaths whenever you feel the impulse to scroll."
        ),
        GoalTemplate(
            templateId = "tmpl_reading",
            category = GoalCategory.READING,
            name = "Read More Books",
            shortDescription = "Turn screen time into reading time",
            icon = "📖",
            defaultUnit = "pages",
            defaultDailyTarget = 10,
            defaultWeeklyTarget = 50,
            recommendedReplacementBehaviours = listOf(
                BEH_READING_5M,
                BEH_READING_10M,
                BEH_PAUSE_10S
            ),
            recommendedTriggerCategories = listOf(
                TriggerCategory.SOCIAL_MEDIA,
                TriggerCategory.VIDEO_STREAMING
            ),
            defaultRewardPreset = RewardPreset.STANDARD,
            defaultDailyEarnCapSeconds = 1800,
            defaultWalletCapSeconds = 3600,
            defaultSessionCapSeconds = 900,
            onboardingCopy = "Anchor your daily reading habit by unlocking screen time after reading a few pages."
        ),
        GoalTemplate(
            templateId = "tmpl_sleep",
            category = GoalCategory.SLEEP,
            name = "Wind Down Better",
            shortDescription = "Protect your bedtime from late-night doomscrolling",
            icon = "😴",
            defaultUnit = "nights",
            defaultDailyTarget = 1,
            defaultWeeklyTarget = 7,
            recommendedReplacementBehaviours = listOf(
                BEH_SLEEP_WINDDOWN,
                BEH_BREATHING_60S,
                BEH_PAUSE_10S
            ),
            recommendedTriggerCategories = listOf(
                TriggerCategory.SOCIAL_MEDIA,
                TriggerCategory.VIDEO_STREAMING
            ),
            defaultRewardPreset = RewardPreset.STRONG,
            defaultDailyEarnCapSeconds = 900,
            defaultWalletCapSeconds = 1800,
            defaultSessionCapSeconds = 600,
            onboardingCopy = "Set a firm evening cutoff for high-dopamine apps to give your nervous system time to settle."
        ),
        GoalTemplate(
            templateId = "tmpl_health",
            category = GoalCategory.HEALTH,
            name = "General Wellbeing",
            shortDescription = "Stay hydrated and take screen breaks",
            icon = "❤️",
            defaultUnit = "actions",
            defaultDailyTarget = 6,
            defaultWeeklyTarget = 30,
            recommendedReplacementBehaviours = listOf(
                BEH_PAUSE_10S,
                BEH_BREATHING_30S
            ),
            recommendedTriggerCategories = listOf(
                TriggerCategory.SOCIAL_MEDIA,
                TriggerCategory.VIDEO_STREAMING
            ),
            defaultRewardPreset = RewardPreset.LIGHT,
            defaultDailyEarnCapSeconds = 1800,
            defaultWalletCapSeconds = 3600,
            defaultSessionCapSeconds = 900,
            onboardingCopy = "Support your mental and physical health with small pauses throughout the day."
        ),
        GoalTemplate(
            templateId = "tmpl_custom",
            category = GoalCategory.CUSTOM,
            name = "Build Custom Habit",
            shortDescription = "Create your own replacement habit and schedule",
            icon = "🌱",
            defaultUnit = "actions",
            defaultDailyTarget = 3,
            defaultWeeklyTarget = 15,
            recommendedReplacementBehaviours = listOf(
                BEH_PAUSE_10S,
                BEH_BREATHING_30S
            ),
            recommendedTriggerCategories = listOf(
                TriggerCategory.CUSTOM
            ),
            defaultRewardPreset = RewardPreset.STANDARD,
            defaultDailyEarnCapSeconds = 2400,
            defaultWalletCapSeconds = 4800,
            defaultSessionCapSeconds = 1200,
            onboardingCopy = "Design a custom routine: pick which apps trigger pauses and which replacement action you complete."
        )
    )

    fun getAllTemplates(): List<GoalTemplate> = templates

    fun getTemplateById(templateId: String): GoalTemplate? =
        templates.firstOrNull { it.templateId == templateId }

    fun getTemplateByCategory(category: GoalCategory): GoalTemplate? =
        templates.firstOrNull { it.category == category }

    fun getAllDistractionRecommendations(): List<DistractionAppRecommendation> = COMMON_DISTRACTIONS

    fun getDistractionsForCategory(category: TriggerCategory): List<DistractionAppRecommendation> =
        COMMON_DISTRACTIONS.filter { it.category == category }

    fun categorizeApp(packageName: String, appLabel: String = ""): TriggerCategory {
        val lower = (packageName + " " + appLabel).lowercase()
        return when {
            lower.contains("instagram") || lower.contains("tiktok") || lower.contains("musically") ||
            lower.contains("facebook") || lower.contains("twitter") || lower.contains("snapchat") ||
            lower.contains("threads") || lower.contains("whatsapp") || lower.contains("telegram") ||
            lower.contains("discord") || lower.contains("messenger") || lower.contains("reddit") ||
            lower.contains("social") -> TriggerCategory.SOCIAL_MEDIA

            lower.contains("youtube") || lower.contains("netflix") || lower.contains("hulu") ||
            lower.contains("disney") || lower.contains("hotstar") || lower.contains("jiocinema") ||
            lower.contains("primevideo") || lower.contains("video") || lower.contains("streaming") ||
            lower.contains("tv") || lower.contains("music") || lower.contains("spotify") ||
            lower.contains("wynk") || lower.contains("gaana") || lower.contains("twitch") ||
            lower.contains("mxplayer") || lower.contains("sonyliv") || lower.contains("zee5") -> TriggerCategory.VIDEO_STREAMING

            lower.contains("game") || lower.contains("gaming") || lower.contains("clash") ||
            lower.contains("pubg") || lower.contains("bgmi") || lower.contains("freefire") ||
            lower.contains("roblox") || lower.contains("candy") || lower.contains("ludo") ||
            lower.contains("chess") || lower.contains("subway") || lower.contains("temple") ||
            lower.contains("supercell") || lower.contains("miniclip") -> TriggerCategory.GAMING

            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("shopping") ||
            lower.contains("myntra") || lower.contains("meesho") || lower.contains("ajio") ||
            lower.contains("nykaa") || lower.contains("ebay") || lower.contains("shopee") ||
            lower.contains("zara") || lower.contains("shein") -> TriggerCategory.SHOPPING

            lower.contains("zomato") || lower.contains("swiggy") || lower.contains("ubereats") ||
            lower.contains("doordash") || lower.contains("blinkit") || lower.contains("zepto") ||
            lower.contains("instamart") || lower.contains("domino") || lower.contains("mcdonald") ||
            lower.contains("food") -> TriggerCategory.FOOD_DELIVERY

            else -> TriggerCategory.CUSTOM
        }
    }
}
