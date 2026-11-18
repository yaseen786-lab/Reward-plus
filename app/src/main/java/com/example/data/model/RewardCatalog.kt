package com.example.data.model

object RewardCatalog {

    private val megaQuizQuestions = listOf(
        QuizQuestion("Which payment system works 24x7 for instant bank transfers in India?", listOf("IMPS", "Cheque", "Demand Draft", "Postal Order"), 0),
        QuizQuestion("What does UPI stand for?", listOf("Unified Payments Interface", "Universal Pay India", "United Personal ID", "Unique Payment Index"), 0),
        QuizQuestion("If 100 points equal ₹1.00, how much is 1,500 points?", listOf("₹10", "₹15", "₹25", "₹50"), 1),
        QuizQuestion("Which planet is known as the Red Planet?", listOf("Venus", "Jupiter", "Mars", "Saturn"), 2),
        QuizQuestion("How many tickets complete 1 Mega Offer in RewardPulse?", listOf("10 tickets", "20 tickets", "30 tickets", "50 tickets"), 2),
        QuizQuestion("What is 25 × 4 + 50?", listOf("100", "125", "150", "175"), 2),
        QuizQuestion("Which gas do plants absorb from the atmosphere?", listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Helium"), 1),
        QuizQuestion("Which Indian city is called the Silicon Valley of India?", listOf("Mumbai", "Pune", "Bengaluru", "Chennai"), 2),
        QuizQuestion("What is the square root of 144?", listOf("11", "12", "13", "14"), 1),
        QuizQuestion("Which symbol starts an IFSC code?", listOf("4 digits", "4 bank letters", "2 numbers", "6 letters"), 1)
    )

    private val normalQuizQuestions = listOf(
        QuizQuestion("How many points do you earn on completing 1 Normal Offer?", listOf("500 pts", "800 pts", "1,000 pts", "1,200 pts"), 1),
        QuizQuestion("What is 15 + 27?", listOf("32", "42", "44", "52"), 1),
        QuizQuestion("Which day comes after Thursday?", listOf("Wednesday", "Saturday", "Friday", "Monday"), 2),
        QuizQuestion("How many hours are in 2 days?", listOf("24", "36", "48", "72"), 2),
        QuizQuestion("Which ocean is the largest on Earth?", listOf("Atlantic", "Indian", "Pacific", "Arctic"), 2),
        QuizQuestion("What is 100 ÷ 4?", listOf("20", "25", "30", "40"), 1),
        QuizQuestion("Which color is NOT in the Indian national flag?", listOf("Saffron", "Green", "Yellow", "Navy Blue"), 2),
        QuizQuestion("How many continents are there on Earth?", listOf("5", "6", "7", "8"), 2)
    )

    // 1 Mega Offer (1500 pts, 30 tickets, 20s/30s/60s ads) + 2 Normal Offers (800 pts, 30 tickets, 5s/7s/10s ads)
    val dailyOffers: List<DailyOfferItem> = listOf(
        DailyOfferItem(
            id = "mega_offer_prime_wallet",
            ticketFieldName = "megaOfferTickets",
            title = "Mega Quiz: Trivia Challenge",
            subtitle = "Play quick trivia rounds & collect 30 tickets for 1,500 pts",
            sponsorName = "Mega Quiz Arena",
            tier = OfferTier.MEGA,
            pointsReward = EconomyConfig.MEGA_OFFER_POINTS,
            adDurationsSeconds = listOf(20, 30, 60),
            quizPool = megaQuizQuestions
        ),
        DailyOfferItem(
            id = "normal_offer_cloud_drive",
            ticketFieldName = "normalOffer1Tickets",
            title = "Normal Quiz 1: Speed Math & GK",
            subtitle = "Answer 2 quick questions per round • 30 tickets = 800 pts",
            sponsorName = "Daily Quiz Sprint",
            tier = OfferTier.NORMAL,
            pointsReward = EconomyConfig.NORMAL_OFFER_POINTS,
            adDurationsSeconds = listOf(5, 7, 10),
            quizPool = normalQuizQuestions
        ),
        DailyOfferItem(
            id = "normal_offer_shop_pulse",
            ticketFieldName = "normalOffer2Tickets",
            title = "Normal Quiz 2: Rapid Trivia",
            subtitle = "Fun quick quiz rounds • Collect 30 tickets for 800 pts",
            sponsorName = "Trivia Pulse",
            tier = OfferTier.NORMAL,
            pointsReward = EconomyConfig.NORMAL_OFFER_POINTS,
            adDurationsSeconds = listOf(5, 7, 10),
            quizPool = normalQuizQuestions.reversed()
        )
    )

    val rewardedAdSlots: List<RewardedAdSlot> = listOf(
        RewardedAdSlot(
            id = "ad_slot_hd_1",
            networkName = "AdMob Rewarded",
            title = "HD Video Spotlight #1",
            pointsReward = EconomyConfig.REWARDED_AD_POINTS,
            watchSeconds = 6
        ),
        RewardedAdSlot(
            id = "ad_slot_hd_2",
            networkName = "AppLovin MAX",
            title = "Interactive Playable Ad #2",
            pointsReward = 350,
            watchSeconds = 8
        ),
        RewardedAdSlot(
            id = "ad_slot_hd_3",
            networkName = "Unity Ads",
            title = "Bonus Brand Video #3",
            pointsReward = 300,
            watchSeconds = 7
        )
    )

    val surveyPartners: List<SurveyPartnerItem> = listOf(
        SurveyPartnerItem(
            id = "pubscale_upi_2026",
            partnerName = "PubScale",
            sourceType = "SURVEY_PUBSCALE",
            topic = "UPI & Digital Payments Habit Study",
            estimatedMinutes = 2,
            pointsReward = 1200,
            qualificationRate = "94% Match",
            questions = listOf(
                SurveyQuestion(
                    prompt = "Which payment rail do you use most often for daily transactions?",
                    options = listOf("UPI Apps", "IMPS / NetBanking", "Debit / Credit Card", "Mobile Wallets")
                ),
                SurveyQuestion(
                    prompt = "How important is instant withdrawal (< 2 mins) when choosing a rewards app?",
                    options = listOf("Essential", "Very Important", "Somewhat Important", "Not Important")
                ),
                SurveyQuestion(
                    prompt = "How frequently do you redeem digital gift vouchers or Google Play codes?",
                    options = listOf("Weekly", "Monthly", "Quarterly", "First time trying")
                )
            )
        ),
        SurveyPartnerItem(
            id = "goodwalls_mobile_2026",
            partnerName = "GoodWalls",
            sourceType = "SURVEY_GOODWALLS",
            topic = "Mobile Shopping & App Discovery Panel",
            estimatedMinutes = 2,
            pointsReward = 1100,
            qualificationRate = "91% Match",
            questions = listOf(
                SurveyQuestion(
                    prompt = "Where do you usually discover new finance or utility Android apps?",
                    options = listOf("Google Play Store", "Reward Offerwalls", "Friend Referrals", "YouTube Reviews")
                ),
                SurveyQuestion(
                    prompt = "What motivates you most to try a partner offer?",
                    options = listOf("High point reward", "Zero upfront fee", "Fast verification", "Trusted brand")
                ),
                SurveyQuestion(
                    prompt = "Would you invite friends for a 500 pts + 40% commission bonus?",
                    options = listOf("Definitely yes", "Likely yes", "Maybe later", "Prefer solo earning")
                )
            )
        ),
        SurveyPartnerItem(
            id = "cpx_fintech_2026",
            partnerName = "CPX Research",
            sourceType = "SURVEY_PARTNER",
            topic = "Consumer Fintech & Telecom Survey",
            estimatedMinutes = 3,
            pointsReward = 950,
            qualificationRate = "87% Match",
            questions = listOf(
                SurveyQuestion(
                    prompt = "What is your preferred monthly mobile data plan tier?",
                    options = listOf("1.5 GB/day", "2 GB/day", "3 GB/day or Unlimited", "Wi-Fi primary")
                ),
                SurveyQuestion(
                    prompt = "Which reward payout option do you prefer most?",
                    options = listOf("Direct UPI / Paytm", "IMPS Bank Transfer", "Google Play Redeem Code", "Mix of all")
                )
            )
        ),
        SurveyPartnerItem(
            id = "bitlabs_gaming_2026",
            partnerName = "BitLabs",
            sourceType = "SURVEY_PARTNER",
            topic = "Play Store Gaming & Entertainment Poll",
            estimatedMinutes = 2,
            pointsReward = 850,
            qualificationRate = "89% Match",
            questions = listOf(
                SurveyQuestion(
                    prompt = "How many hours per week do you play mobile games?",
                    options = listOf("Under 2 hours", "2–5 hours", "5–10 hours", "10+ hours")
                ),
                SurveyQuestion(
                    prompt = "What do you redeem Google Play codes for?",
                    options = listOf("In-game passes", "Paid apps", "OTT / subscriptions", "Gift to friends")
                )
            )
        )
    )
}
