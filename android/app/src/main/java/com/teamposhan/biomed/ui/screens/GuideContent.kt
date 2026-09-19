package com.teamposhan.biomed.ui.screens

import com.teamposhan.biomed.ui.components.GuideStep
import com.teamposhan.biomed.ui.components.WhyContent

/**
 * The walkthrough text for all three modules.
 *
 * Written to one rule: **a person who cannot read fluently should still get it right.** So
 * every step is one action, the words are the ones used at home rather than in a clinic, and
 * the deciding instruction is repeated as a tick-or-cross line beside a picture.
 *
 * Vocabulary that is deliberately absent from anything on these screens: developmental,
 * milestone, gaze, fixation, saccade, prophylaxis, deficiency, melanin, colorimetry,
 * calibration, rickets. Each has a plain replacement below. The real terms are not hidden —
 * they live one tap away behind the "Why does this matter?" button, which is where somebody
 * who wants them will look.
 */
object GuideContent {

    // =====================================================================
    // Module 1 — growth and everyday skills
    // =====================================================================

    val module1: List<GuideStep> = listOf(
        GuideStep(
            artKey = "guide_m1_sit_together",
            title = "Sit together somewhere calm",
            body = "You will answer questions about what your child already does at home. " +
                "There is no right or wrong answer. Nobody is being tested.",
            doThis = "Sit where your child can play near you",
            dontDoThis = "Do not do this while the child is crying or hungry",
        ),
        GuideStep(
            artKey = "guide_m1_answer_honestly",
            title = "Answer what you have really seen",
            body = "Think about the last few days. If you have seen your child do it, say " +
                "Yes. If they are starting to, say Maybe. If not yet, say No.",
            doThis = "Answer from what you have actually seen",
            dontDoThis = "Do not guess to make the answer look better",
        ),
        GuideStep(
            artKey = "guide_m1_not_sure",
            title = "\"Not sure\" is a real answer",
            body = "If you have never had the chance to see it, choose Not sure. That is " +
                "useful to us — it tells the health worker what to watch for. It is not a " +
                "mark against you or your child.",
            doThis = "Choose Not sure when you have not seen it",
        ),
        GuideStep(
            artKey = "guide_m1_measure_weight",
            title = "Weight and height",
            body = "Weigh the child with light clothes and no shoes. For height, under two " +
                "years measure lying down; over two years measure standing against a wall " +
                "with heels flat.",
            doThis = "Remove shoes, caps and heavy clothes first",
            dontDoThis = "Do not measure a child who is being held",
        ),
        GuideStep(
            artKey = "guide_m1_measure_arm",
            title = "The arm tape",
            body = "Put the tape around the middle of the left upper arm, halfway between " +
                "the shoulder and the elbow. The arm should hang loose. The tape should sit " +
                "flat — snug, not tight.",
            doThis = "Let the arm hang relaxed by the side",
            dontDoThis = "Do not pull the tape tight into the skin",
        ),
        GuideStep(
            artKey = "guide_m1_no_internet",
            title = "It all works without internet",
            body = "Everything is saved on this phone straight away. When the phone finds " +
                "a network later, it sends everything on its own. You never have to wait.",
        ),
    )

    val whyModule1 = WhyContent(
        title = "Why these questions?",
        plain = "Children pick up new skills in roughly the same order — lifting the head, " +
            "sitting, babbling, walking, talking. Asking what a child does now, and " +
            "comparing it with what most children do at that age, is the earliest and " +
            "cheapest way to spot a child who could use extra help.",
        technical = "Answers are scored Yes = 1, Maybe = 0.5, No = 0, Not sure = 0 plus a " +
            "flag. Each age band up to the child's age is asked, not only their own band, " +
            "because a skill missed from an earlier band is already late rather than merely " +
            "approaching. Growth uses the WHO Child Growth Standards.",
        evidence = "AIIMS Delhi developmental milestones chart; WHO Child Growth Standards " +
            "2006.",
        limitation = "This is a check, not a diagnosis. A result that says look closer " +
            "means exactly that — see a doctor, who has tests this app does not.",
    )

    // =====================================================================
    // Module 2 — how the child looks and pays attention
    // =====================================================================

    val module2: List<GuideStep> = listOf(
        GuideStep(
            artKey = "guide_m2_what_happens",
            title = "Your child will watch a short video",
            body = "Simple pictures will move on the screen — a friendly face on one side, " +
                "a moving toy on the other. The front camera watches where your child " +
                "looks. It takes about two minutes.",
            doThis = "Let your child watch naturally",
        ),
        GuideStep(
            artKey = "guide_m2_no_photos",
            title = "No pictures are kept",
            body = "The camera is only used to see which way the eyes are pointing. No " +
                "photo and no video of your child is saved anywhere, on the phone or " +
                "outside it. Only numbers are kept.",
        ),
        GuideStep(
            artKey = "guide_m2_landscape",
            title = "Turn the phone sideways",
            body = "This part of the check needs the phone on its side, like watching a " +
                "video. The pictures are shown side by side, and sideways makes each one " +
                "big enough for your child to see properly.",
            doThis = "Turn the phone to landscape before you start",
            dontDoThis = "Do not hold it upright — the pictures become too small",
        ),
        GuideStep(
            artKey = "guide_m2_distance",
            title = "Prop it one forearm away",
            body = "Measure with your own arm: elbow to fingertips is about right. Stand " +
                "it against a wall, a box or a stack of books so it is facing your child " +
                "at the same height as their eyes.",
            doThis = "Propped up, elbow-to-fingertips away, level with the eyes",
            dontDoThis = "Do not hold it in your hand — even small movements spoil the check",
        ),
        GuideStep(
            artKey = "guide_m2_lighting",
            title = "Light on the face, not behind",
            body = "Stand so the window or the lamp is behind the phone, shining onto your " +
                "child's face. If the light is behind the child, their face goes dark and " +
                "the camera cannot see the eyes.",
            doThis = "Light behind the phone, face lit up",
            dontDoThis = "Do not sit with a bright window behind the child",
        ),
        GuideStep(
            artKey = "guide_m2_steady",
            title = "Keep the phone still",
            body = "Once it is propped, leave it alone. If the phone moves, the app cannot " +
                "tell whether your child moved or the phone did — and it will treat your " +
                "movement as theirs.",
            doThis = "Set it down and step behind it",
            dontDoThis = "Do not pick it up, tilt it or adjust it once the check has started",
        ),
        GuideStep(
            artKey = "guide_m2_quiet_room",
            title = "Quiet room, nobody behind the phone",
            body = "Switch off the TV and radio. Ask other children to wait outside the " +
                "room. Anything interesting behind the phone will pull your child's eyes " +
                "away from the screen.",
            doThis = "One adult, one child, quiet room",
            dontDoThis = "Do not let others stand behind the phone",
        ),
        GuideStep(
            artKey = "guide_m2_your_role",
            title = "Stay quiet until the app asks",
            body = "Sit behind the phone, out of sight. At one point the app will ask you " +
                "to call your child's name — say it once, in your normal voice. Do not " +
                "point at the screen or tap it.",
            doThis = "Call the name once, normally, when asked",
            dontDoThis = "Do not point, clap or say \"look here\"",
        ),
        GuideStep(
            artKey = "guide_m2_green_box",
            title = "Watch for the green box",
            body = "A small camera window shows your child's face. When the app can see " +
                "them properly the box turns green. If it is not green, move the phone a " +
                "little or turn towards the light.",
            doThis = "Wait for green before starting",
        ),
        GuideStep(
            artKey = "guide_m2_stop_anytime",
            title = "You can stop at any time",
            body = "If your child gets upset or bored, press Stop. Nothing is lost and you " +
                "can start again later. If we do not see enough, the app will simply say " +
                "the check did not work — that never means something is wrong with your " +
                "child.",
            doThis = "Stop whenever your child has had enough",
        ),
    )

    val whyModule2 = WhyContent(
        title = "Why watch where a child looks?",
        plain = "Long before children can speak, they show what interests them by where " +
            "they look. Children who look more at faces and turn when their name is called " +
            "are showing the early social attention that speech is built on. Watching this " +
            "is a way to notice, very early, a child who might need extra support.",
        technical = "The front camera runs a face-landmark model on the device. From the " +
            "landmarks the app estimates head direction and eye direction and works out " +
            "which half of the screen is being looked at, plus how long looking is held and " +
            "how often the child responds to their name. Frames are processed and thrown " +
            "away; nothing is stored or uploaded.",
        evidence = "Paired social-versus-object preference tasks and name-call response, " +
            "as used in published infant screening studies.",
        limitation = "A phone camera runs at about 15 to 30 pictures a second, which is " +
            "far too slow to measure fast eye jumps. So the app only reports slow, robust " +
            "things: which side was looked at, and for how long. It is never a diagnosis, " +
            "and a child who was tired or restless will simply give an unusable result.",
    )

    // =====================================================================
    // Module 3 — sunlight, drops and skin tone
    // =====================================================================

    val module3: List<GuideStep> = listOf(
        GuideStep(
            artKey = "guide_m3_what_this_is",
            title = "This is about sunlight and drops",
            body = "The body makes vitamin D from sunlight on bare skin. Babies also need " +
                "daily drops. This module checks three things: is the baby getting the " +
                "drops, when is the sun right where you live, and how long should this " +
                "child stay out in it.",
        ),
        GuideStep(
            artKey = "guide_m3_not_a_blood_test",
            title = "This is NOT a blood test",
            body = "The phone cannot tell you whether your child's vitamin D is low or " +
                "high. Nothing here does that. Only a blood test can, and for a healthy " +
                "child a blood test is not needed.",
            dontDoThis = "Do not treat anything here as a vitamin D level",
        ),
        GuideStep(
            artKey = "guide_m3_daily_drops",
            title = "Every baby under one year needs the drops",
            body = "This is true for every baby, breastfed or not, everywhere. Breastmilk " +
                "is the best food for a baby but it has very little vitamin D in it. That " +
                "is normal and it is not the mother's fault.",
            doThis = "One dose of the drops every day, until the first birthday",
            dontDoThis = "Never give a bigger dose to catch up a missed day",
        ),
        GuideStep(
            artKey = "guide_m3_grey_card",
            title = "You will need the grey card",
            body = "It is the plain grey card that comes with the kit. The phone uses it " +
                "to understand what colour the light is. Without it, the same child looks " +
                "different under a yellow bulb and in daylight.",
            doThis = "Keep the grey card clean and flat",
            dontDoThis = "Do not use white paper or a grey cloth instead",
        ),
        GuideStep(
            artKey = "guide_m3_shade",
            title = "Go into the shade",
            body = "Stand in shade, or indoors near an open door. Bright direct sun makes " +
                "the skin shine, and the phone reads shine as skin — which gives the wrong " +
                "answer.",
            doThis = "Even, soft light — shade or near a doorway",
            dontDoThis = "Do not stand in direct sunlight or under a coloured light",
        ),
        GuideStep(
            artKey = "guide_m3_inner_arm",
            title = "Use the inside of the arm",
            body = "Turn the child's arm over and use the soft inner side, near the elbow. " +
                "That patch is almost never tanned, so it shows the child's own natural " +
                "skin tone.",
            doThis = "Inner forearm, near the elbow",
            dontDoThis = "Do not use the face, the back of the hand, or a sun-tanned patch",
        ),
        GuideStep(
            artKey = "guide_m3_clean_skin",
            title = "Wipe the skin first",
            body = "Gently wipe away oil, powder, cream or dirt with a dry cloth and wait a " +
                "moment. Oil makes the skin shine and powder makes it look lighter than it " +
                "is.",
            doThis = "Clean, dry, bare skin",
            dontDoThis = "Do not measure over oil, powder, cream or mehndi",
        ),
        GuideStep(
            artKey = "guide_m3_two_boxes",
            title = "Fill both boxes on the screen",
            body = "Two boxes appear. Put the child's inner arm in the left box and lay the " +
                "grey card flat beside it in the right box. Hold the phone about one hand-" +
                "span away and keep it steady.",
            doThis = "Arm in the left box, grey card in the right box",
            dontDoThis = "Do not let your fingers or a shadow fall inside either box",
        ),
        GuideStep(
            artKey = "guide_m3_wait_green",
            title = "Wait until both boxes turn green",
            body = "Green means the phone is happy with what it can see. If a box stays " +
                "white, move a little closer, flatten the card, or step into better light. " +
                "The button only works once the reading is good.",
            doThis = "Wait for both boxes to be green, then press the button",
        ),
        GuideStep(
            artKey = "guide_m3_what_you_get",
            title = "What you get at the end",
            body = "One thing: how many minutes in the midday sun suit this child. Darker " +
                "skin needs longer — that is the skin protecting itself from burning, not " +
                "a problem. You can open the working at the bottom to see every step and " +
                "every number the phone used.",
            doThis = "Tap \"See the working\" any time you want the details",
        ),
    )

    val whyModule3 = WhyContent(
        title = "What is this module doing?",
        plain = "Two separate things. First, making sure every baby under one gets their " +
            "daily drops, because that is what actually prevents the problem. Second, " +
            "working out how long this particular child should be outside, because the " +
            "colour of a child's skin changes how quickly sunlight turns into vitamin D.",
        technical = "One daily dose, birth to twelve months, for every infant " +
            "regardless of feeding. The camera measures skin tone as an angle (ITA), " +
            "corrected for the colour of the light using a grey card, and that angle scales " +
            "the suggested minutes outdoors. There is no risk score anywhere in this " +
            "module, because no scoring questionnaire for under-fives has ever been " +
            "validated.",
        evidence = "Indian Academy of Paediatrics 2021; Global Consensus on Prevention and " +
            "Management of Nutritional Rickets 2016; RBSK screening card item B5.",
        limitation = "Skin tone cannot tell you a child's vitamin D level — against real " +
            "blood tests it explains only about one twentieth of the difference between " +
            "children. It is used for one thing only: how long to stay in the sun.",
    )

    /**
     * Split points for module 3.
     *
     * The first three steps set expectations and are shown once, when the module opens. The
     * rest are capture instructions and are shown again every time the camera is opened,
     * because that is the moment they apply and a walkthrough read ten minutes earlier has
     * already been forgotten.
     */
    val module3Intro: List<GuideStep> get() = module3.take(3)
    val module3Capture: List<GuideStep> get() = module3.drop(3)
}
