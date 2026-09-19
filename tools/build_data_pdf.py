"""Builds the Poshan demographic / epidemiological evidence PDF."""
import os
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate, Frame, PageTemplate, Paragraph, Spacer, Table, TableStyle,
    PageBreak, KeepTogether,
)

OUT = r"C:\Users\jagad\Downloads\dev\teamPoshan\docs\Poshan-India-Child-Development-Data.pdf"
os.makedirs(os.path.dirname(OUT), exist_ok=True)

TEAL = colors.HexColor("#0E6B5C")
TEAL_SOFT = colors.HexColor("#E4F5F0")
AMBER = colors.HexColor("#B87414")
INK = colors.HexColor("#14201D")
INK_SOFT = colors.HexColor("#4A5A56")
LINE = colors.HexColor("#D8D4CC")
RED = colors.HexColor("#B3231F")
SAND = colors.HexColor("#FBF9F5")

ss = getSampleStyleSheet()


def st(name, **kw):
    base = kw.pop("parent", ss["BodyText"])
    return ParagraphStyle(name, parent=base, **kw)


H1 = st("H1", parent=ss["Heading1"], fontName="Helvetica-Bold", fontSize=21,
        textColor=TEAL, spaceAfter=4, leading=25)
H2 = st("H2", parent=ss["Heading2"], fontName="Helvetica-Bold", fontSize=14,
        textColor=TEAL, spaceBefore=14, spaceAfter=6, leading=17)
H3 = st("H3", parent=ss["Heading3"], fontName="Helvetica-Bold", fontSize=11,
        textColor=INK, spaceBefore=10, spaceAfter=4)
BODY = st("BODY", fontSize=9.6, leading=14, textColor=INK, alignment=TA_LEFT)
SMALL = st("SMALL", fontSize=8.2, leading=11.4, textColor=INK_SOFT)
TINY = st("TINY", fontSize=7.2, leading=9.6, textColor=INK_SOFT)
CELL = st("CELL", fontSize=8.4, leading=11, textColor=INK)
CELL_B = st("CELL_B", fontSize=8.4, leading=11, textColor=INK, fontName="Helvetica-Bold")
BULLET = st("BULLET", fontSize=9.4, leading=13.4, leftIndent=10, bulletIndent=1,
            textColor=INK)


def table(rows, widths, header=True, aligns=None):
    data = []
    for r_i, row in enumerate(rows):
        line = []
        for c_i, cell in enumerate(row):
            if r_i == 0 and header:
                line.append(Paragraph(str(cell), st("th", fontSize=8.4, leading=11,
                                                    textColor=colors.white,
                                                    fontName="Helvetica-Bold")))
            else:
                style = CELL_B if (aligns and aligns[c_i] == "b") else CELL
                line.append(Paragraph(str(cell), style))
        data.append(line)
    t = Table(data, colWidths=widths, repeatRows=1 if header else 0)
    style = [
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("LINEBELOW", (0, 0), (-1, -2), 0.4, LINE),
    ]
    if header:
        style += [
            ("BACKGROUND", (0, 0), (-1, 0), TEAL),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, SAND]),
        ]
    else:
        style += [("ROWBACKGROUNDS", (0, 0), (-1, -1), [colors.white, SAND])]
    t.setStyle(TableStyle(style))
    return t


def callout(title, body, colour=TEAL_SOFT, border=TEAL):
    inner = [[Paragraph(title, st("ct", fontName="Helvetica-Bold", fontSize=10,
                                  textColor=border, leading=13))],
             [Paragraph(body, BODY)]]
    t = Table(inner, colWidths=[168 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colour),
        ("LINEBEFORE", (0, 0), (0, -1), 3, border),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
        ("RIGHTPADDING", (0, 0), (-1, -1), 10),
    ]))
    return t


story = []

# ---------------------------------------------------------------- cover
story += [
    Spacer(1, 26 * mm),
    Paragraph("Poshan", st("cov1", fontName="Helvetica-Bold", fontSize=40,
                           textColor=TEAL, leading=44)),
    Paragraph("Early Childhood Development &amp; Nutrition Screening",
              st("cov2", fontSize=14, textColor=INK_SOFT, leading=19)),
    Spacer(1, 10 * mm),
    Paragraph("India - Children 0-5 years", st("cov3", fontName="Helvetica-Bold",
                                               fontSize=17, textColor=INK, leading=22)),
    Paragraph("Demographic and epidemiological evidence base",
              st("cov4", fontSize=12, textColor=AMBER, leading=16)),
    Spacer(1, 14 * mm),
]
story.append(callout(
    "Why a screening platform is needed",
    "India has roughly <b>114 million children under five</b>. Measured national surveys put "
    "<b>35.5% stunted</b>, <b>19.3% wasted</b>, <b>32.1% underweight</b> and <b>67.1% anaemic</b>. "
    "Community studies of developmental delay report <b>10-12%</b> of under-fives affected, and "
    "the Government's own screening programme identifies a defect or delay in about <b>7.4%</b> of "
    "children screened - yet only <b>47%</b> of those identified go on to receive care. "
    "The bottleneck is not treatment capacity alone; it is <b>early identification in the community</b>."
))
story += [
    Spacer(1, 8 * mm),
    Paragraph("Compiled September 2026 from NFHS-5 (2019-21), NFHS-6 (2023-24), CNNS (2016-18), "
              "Census 2011, UN WPP 2024, WHO/UNICEF, GBD, RBSK programme data and peer-reviewed "
              "Indian studies. Figures marked <b>[modelled]</b> are statistical estimates; "
              "everything else is measured survey or programme data.",
              SMALL),
    Paragraph("Prepared for Team Poshan - Bio-Med competition submission", TINY),
    PageBreak(),
]

# ---------------------------------------------------------------- summary
story += [Paragraph("At a glance", H1),
          Paragraph("The burden, and the size of the gap, in one table.", SMALL),
          Spacer(1, 5 * mm)]

summary_rows = [
    ["Indicator", "Prevalence", "Est. children affected", "Source"],
    ["Stunting (height-for-age &lt; -2 SD)", "35.5%", "~ 40.4 million", "NFHS-5"],
    ["Wasting (weight-for-height &lt; -2 SD)", "19.3%", "~ 21.9 million", "NFHS-5"],
    ["Severe wasting", "7.7%", "~ 8.8 million", "NFHS-5"],
    ["Underweight (weight-for-age &lt; -2 SD)", "32.1%", "~ 36.5 million", "NFHS-5"],
    ["Anaemia (6-59 months)", "67.1%", "~ 76.3 million", "NFHS-5"],
    ["Developmental delay (community studies)", "10-12.2%", "~ 11.4 - 13.9 million", "Indian J Pediatrics 2024; Agarwal 2018"],
    ["Speech / language delay", "2.53% (1-12 yr)", "-", "e-CACD 2024 review"],
    ["Cerebral palsy", "2.95 per 1,000", "~ 335,000", "Chauhan 2019, Indian J Pediatr"],
    ["Autism spectrum disorder", "1 in 65 (2-9 yr, est.)", "~ 1.7 million [estimate]", "Cureus 2024 review"],
    ["Vitamin D deficiency (1-4 yr, serum)", "13.7%", "~ 15.6 million", "CNNS 2016-18"],
    ["Low birth weight", "18.2% of births", "~ 4.2 million / year", "NFHS-5"],
    ["Full immunisation (12-23 months)", "76.4% (so 23.6% incomplete)", "~ 5.4 million / year", "NFHS-5"],
    ["Low birth weight (modelled)", "~27%", "-", "UNICEF-WHO 2020 [modelled]"],
]

story += [table(summary_rows, [58 * mm, 30 * mm, 40 * mm, 40 * mm]), Spacer(1, 3 * mm),
          Paragraph("Absolute figures are computed from NFHS-5 percentages applied to the "
                    "UN WPP 2024 under-five population of 113.7 million, except where a source "
                    "gives its own denominator. Treated as planning estimates, not measurements.", TINY),
          PageBreak()]

# ---------------------------------------------------------------- denominators
story += [Paragraph("1. Population denominators", H1),
          Spacer(1, 3 * mm),
          table([
              ["Measure", "Value", "Year", "Source"],
              ["Children aged 0-4", "113.7 million", "2024", "UN WPP 2024 / World Bank"],
              ["Children aged 0-6", "164.5 million", "2011", "Census of India"],
              ["Annual births", "~ 23 million", "2023", "SRS 2023 (MoSPI)"],
              ["Under-five mortality rate", "32 per 1,000 live births", "2020", "SRS / UNICEF"],
          ], [52 * mm, 42 * mm, 22 * mm, 52 * mm]),
          Spacer(1, 5 * mm),
          Paragraph("India carries the largest absolute burden of stunted and wasted children of "
                    "any country. Because the denominator is so large, even a single percentage "
                    "point is over a million children.", BODY),
          PageBreak()]

# ---------------------------------------------------------------- nutrition
story += [Paragraph("2. Growth and nutrition (Module 1 &amp; 3 inputs)", H1),
          Paragraph("All measured, NFHS-5 (2019-21) unless stated.", SMALL), Spacer(1, 4 * mm)]

story += [Paragraph("2.1 Anthropometry", H3),
          table([
              ["Indicator", "NFHS-5", "NFHS-4", "Rural", "Urban"],
              ["Stunting", "35.5%", "38.4%", "37.3%", "30.1%"],
              ["Wasting", "19.3%", "21.0%", "19.5%", "18.5%"],
              ["Severe wasting", "7.7%", "7.5%", "7.7%", "7.6%"],
              ["Underweight", "32.1%", "35.8%", "33.8%", "27.3%"],
              ["Anaemia (6-59 mo)", "67.1%", "58.6%", "68.3%", "64.2%"],
          ], [50 * mm, 26 * mm, 26 * mm, 26 * mm, 26 * mm]),
          Spacer(1, 3 * mm),
          Paragraph("NFHS-6 (2023-24) shows improvement: stunting 29.3%, underweight 31.8%, "
                    "wasting 19.0%, severe wasting 5.2%. Anaemia remains the least improved indicator.", SMALL)]

story += [Paragraph("2.2 South India - state-level stunting", H3),
          table([
              ["State", "NFHS-5", "NFHS-6"],
              ["Karnataka", "35.4%", "26.5%"],
              ["Telangana", "33.1%", "27.0%"],
              ["Andhra Pradesh", "31.2%", "24.6%"],
              ["Tamil Nadu", "25.0%", "20.7%"],
              ["Kerala", "23.4%", "20.1%"],
          ], [56 * mm, 40 * mm, 40 * mm]),
          Spacer(1, 3 * mm),
          Paragraph("Even in the best-performing southern state, roughly one child in five is "
                    "stunted - a reminder that this is not a problem confined to any region.", SMALL)]

story += [Paragraph("2.3 MUAC and acute malnutrition", H3),
          Paragraph(
              "NFHS does not measure MUAC, so there is <b>no nationally representative MUAC "
              "prevalence</b> for India. The best community measurement available is a tribal "
              "Maharashtra study (2021-22, ages 0-59 months): <b>SAM 1.4%</b> (MUAC &lt; 115 mm), "
              "<b>MAM 9.8%</b> (115-125 mm), total acute malnutrition <b>11.2%</b>. "
              "Applying the NFHS-5 severe-wasting rate suggests roughly <b>8.8 million</b> children "
              "at SAM-equivalent severity, while the Government's Poshan Tracker identified fewer "
              "than <b>1 million</b> SAM children in 2023-24 - a large detection gap. "
              "This is precisely the gap a community MUAC tool addresses.", BODY)]

story += [Paragraph("2.4 Infant and young child feeding", H3),
          table([
              ["Indicator", "NFHS-5", "NFHS-4"],
              ["Exclusive breastfeeding (0-5 mo)", "63.7%", "54.9%"],
              ["Timely complementary feeding (6-8 mo)", "45.9%", "42.7%"],
              ["Minimum acceptable diet (6-23 mo)", "11.3%", "9.6%"],
          ], [80 * mm, 28 * mm, 28 * mm]),
          Spacer(1, 2 * mm),
          Paragraph("Only about one in nine young children receives a minimally acceptable diet - "
                    "the single strongest modifiable driver of stunting in this age band.", SMALL)]

story += [Paragraph("2.5 Vitamin D", H3),
          table([
              ["Measure", "Value", "Population", "Source"],
              ["Serum vitamin D deficiency", "13.7%", "Ages 1-4", "CNNS 2016-18"],
              ["Serum deficiency", "18.2%", "Ages 5-9", "CNNS 2016-18"],
              ["Pooled clinic/lab estimate", "66.4% [heterogeneous]", "Children (meta-analysis)", "Meta-analysis 2025"],
              ["South India (all ages)", "51.6%", "Diagnostic-lab data", "2025"],
          ], [52 * mm, 40 * mm, 40 * mm, 36 * mm]),
          Spacer(1, 2 * mm),
          Paragraph("The gap between the measured national figure (13.7%) and clinic-based estimates "
                    "(51-66%) reflects who gets tested - symptomatic children. Rickets has "
                    "<b>no national prevalence estimate</b>; WHO treats &gt;1% as a public-health "
                    "concern. This heterogeneity is why Poshan deliberately frames Module 3 as a "
                    "<b>risk index with referral</b>, never as a measurement.", SMALL)]

story += [Paragraph("2.6 Immunisation", H3),
          table([
              ["Vaccine (12-23 mo)", "Coverage", "Drop-out"],
              ["BCG", "95.2%", "BCG -> MCV1: 8.6%"],
              ["DPT / pentavalent 3", "86.7%", "DPT1 -> DPT3: 7.4%"],
              ["Polio 3", "80.5%", "OPV1 -> OPV3: 12.9%"],
              ["Measles MCV1", "87.9%", "-"],
              ["Measles MCV2", "31.9%", "-"],
              ["Fully vaccinated for age", "76.4%", "-"],
          ], [58 * mm, 34 * mm, 44 * mm]),
          Spacer(1, 2 * mm),
          Paragraph("Roughly <b>one child in four</b> is not fully vaccinated, and the MCV2 figure "
                    "shows how sharply coverage falls for later doses - the case for the app's "
                    "due/overdue tracking rather than a static schedule.", SMALL),
          PageBreak()]

# ---------------------------------------------------------------- development
story += [Paragraph("3. Developmental delay and disability (Module 2 inputs)", H1),
          Paragraph("These figures are more heterogeneous than the nutrition data: India has no "
                    "nationally measured developmental-delay prevalence, only community and "
                    "hospital studies. Ranges are given deliberately.", SMALL), Spacer(1, 4 * mm)]

story += [table([
    ["Condition", "Prevalence", "Notes / population", "Source"],
    ["Developmental delay, under 5", "10% (95% CI 6.8-14)", "Community study", "Indian J Pediatrics 2024"],
    ["Developmental delay, under 5", "12.2%", "Community", "Agarwal 2018"],
    ["Developmental delay, under 2", "1.5-2.5%", "Indian studies", "Chand 2025 review"],
    ["At risk of poor development (LMIC)", "~43%", "~250 million children globally [modelled]", "Lancet 2016; 2020"],
    ["Developmental disability, global", "52.9 million under 5", "95% live in LMICs [modelled]", "GBD 2016"],
    ["Speech / language delay", "2.53%", "Ages 1-12 years", "e-CACD review"],
    ["Language delay share", "38.3% of delayed children", "Most common domain", "JCC Practice 2025"],
    ["Cerebral palsy", "2.95 per 1,000", "Pooled Indian estimate", "Chauhan 2019"],
    ["Hypoxic-ischaemic encephalopathy", "10-15 per 1,000 births", "vs 1-3 globally", "Hospital studies"],
    ["Autism, ages 2-9", "1 in 65 (~1.5%)", "~ 1.8-2 million children [estimate]", "Cureus 2024"],
    ["Autism, ages 1-10", "1.5 per 1,000", "Multi-site population study", "Raina 2016"],
    ["Neonatal hearing loss", "1-8 per 1,000 screened", "Protocol-dependent", "PMC6127065"],
    ["Childhood blindness", "0.8 per 1,000", "Systematic review", "Wadhwani 2020"],
], [46 * mm, 32 * mm, 46 * mm, 44 * mm]),
    Spacer(1, 4 * mm),
    callout("Read the delay figures with care",
            "The 10-12% community estimates and the 1.5-2.5% under-two estimates measure different "
            "things: the first screens for <i>any</i> domain falling behind, the second for "
            "established delay. Indian estimates come from single-site studies with wide "
            "heterogeneity - there is no national measured prevalence. Any claim of clinical "
            "accuracy should be framed as screening risk stratification, not diagnosis.",
            SAND, AMBER)]

story += [Paragraph("3.1 What the national screening programme finds (RBSK)", H3),
          table([
              ["RBSK indicator", "Value", "Period"],
              ["Children screened", "160.84 crore screenings", "2014-2024"],
              ["Children identified with a defect", "11.90 crore (7.4%)", "2014-2024"],
              ["Of those, received treatment", "5.64 crore (47.4%)", "2014-2024"],
              ["Districts with a DEIC", "53.75%", "2024"],
          ], [72 * mm, 50 * mm, 26 * mm]),
          Spacer(1, 2 * mm),
          Paragraph("RBSK screens children for the four Ds - defects at birth, deficiencies, diseases, "
                    "and developmental delays and disabilities - at Anganwadi centres and schools. "
                    "Detection rates vary enormously by state (1.7% Bihar to 12.3% Gujarat), which "
                    "points to reporting and screening-quality differences rather than true "
                    "variation. A standardised, guided digital tool directly addresses that "
                    "inconsistency.", SMALL)]

story += [Paragraph("3.2 The early-intervention gap", H3),
          Paragraph(
              "Identification is only useful if it leads to care. RBSK data shows that "
              "<b>fewer than half (47.4%)</b> of children identified ever receive secondary or "
              "tertiary care, and District Early Intervention Centres exist in only <b>53.75%</b> "
              "of districts. Multi-country work on early-intervention access reports that only "
              "<b>18.9%</b> of families access packaged early intervention, with <b>75.5%</b> "
              "unmet need. In India, where a community screening tool can shorten the path from "
              "home to DEIC, the leverage is in the first step: getting the child identified "
              "and referred at all.", BODY)]
story.append(PageBreak())

# ---------------------------------------------------------------- causes
story += [Paragraph("4. Why children are affected - the causal picture", H1),
          Spacer(1, 3 * mm),
          table([
              ["Risk factor", "Indian evidence", "Link to developmental outcome"],
              ["Stunting", "35.5% of under-fives (NFHS-5)", "Chronic undernutrition; consistent association with poorer cognitive and motor scores"],
              ["Low birth weight", "18.2% of births (NFHS-5); OR 1.46 for stunting", "The strongest early predictor of later delay"],
              ["Anaemia", "67.1% of children 6-59 months", "Iron deficiency impairs motor and cognitive development"],
              ["Birth asphyxia / HIE", "HIE 10-15 per 1,000 births", "Leading cause of cerebral palsy in India"],
              ["Prematurity", "Rising with improved neonatal survival", "Corrected-age screening essential under 24 months"],
              ["Iodine deficiency", "Historic national programme; still regionally present", "Preventable cause of intellectual disability"],
              ["Vitamin D deficiency", "13.7% measured (1-4 yr)", "Rickets, delayed motor milestones"],
              ["Feeding practices", "Only 11.3% get a minimum acceptable diet", "Direct pathway to stunting and micronutrient deficiency"],
              ["Early stimulation deficit", "~43% of LMIC under-fives at risk", "Poverty, low maternal education and neglect reduce cognitive stimulation"],
              ["Maternal education / poverty", "NFHS-5 wealth and education gradients", "Cross-cutting determinants of both nutrition and stimulation"],
          ], [38 * mm, 56 * mm, 74 * mm]),
          Spacer(1, 4 * mm),
          Paragraph("The pattern that matters for design: these causes are <b>overlapping and "
                    "cumulative</b>. A low-birth-weight, stunted, anaemic child in a low-stimulation "
                    "household carries risk on several axes at once - which is why Poshan produces "
                    "a <b>single composite risk score</b> rather than four independent verdicts.", BODY)]
story.append(PageBreak())

# ---------------------------------------------------------------- mapping
story += [Paragraph("5. How this evidence maps to the Poshan modules", H1),
          Spacer(1, 3 * mm),
          table([
              ["Module", "Burden it addresses", "Evidence anchor"],
              ["Module 1 - Developmental milestones<br/>(123 items, 4 domains, 9 age bands)",
               "10-12% of under-fives with developmental delay; language delay is the most common "
               "single domain (38.3% of delayed children)",
               "AIIMS red-flag chart; question bank; RBSK detection variability"],
              ["Module 2 - Attention &amp; gaze<br/>(iris gaze, fixations, response-to-name, "
               "joint attention, touch task)",
               "Autism 1 in 65 (2-9 yr, est.); the earliest behavioural markers are social "
               "attention and name response, which precede language delay",
               "GeoPref; SenseToKnow (AUC 0.92 at home); Gazefinder; gap-overlap"],
              ["Module 3 - Nutrition &amp; growth<br/>(WHO z-scores, MUAC, Vitamin D risk)",
               "35.5% stunted, 19.3% wasted, 7.7% severely wasted, 67.1% anaemic, "
               "SAM detection gap of ~8 million children",
               "WHO growth standards; NFHS-5; MUAC cut-offs"],
              ["Immunisation tracking",
               "23.6% not fully vaccinated; MCV2 coverage only 31.9%",
               "National Immunisation Schedule; NFHS-5 drop-out rates"],
          ], [46 * mm, 60 * mm, 62 * mm]),

          Spacer(1, 6 * mm),
          callout("The design conclusion",
                  "The burden in India is large in absolute numbers but the tools that reach it are "
                  "scarce: DEICs cover just over half of districts, and under half of identified "
                  "children receive care. The highest-leverage intervention is therefore not a more "
                  "accurate test - it is a <b>cheap, offline, guided screen that any Anganwadi "
                  "worker can run in a home</b>, that produces a referral when one is needed and "
                  "explicitly says \"inconclusive, repeat\" when the data does not support a "
                  "conclusion.",
                  TEAL_SOFT, TEAL),
          PageBreak()]

# ---------------------------------------------------------------- daily
story += [Paragraph("6. Daily increment - how fast the affected population is replenished", H1),
          Paragraph("India records roughly <b>23 million births a year</b> (SRS 2023), which is "
                    "<b>~ 63,000 births every day</b>. Applying the measured prevalence rates to "
                    "that daily birth cohort gives the number of newly affected children entering "
                    "each group per day. This is an <b>inflow</b> calculation, not a measured daily "
                    "count.", SMALL),
          Spacer(1, 5 * mm),
          table([
              ["Indicator", "Rate applied", "Per day", "Per month", "Per year"],
              ["Births", "-", "~ 63,014", "~ 1.89 million", "~ 23 million"],
              ["Low birth weight", "18.2% of births", "~ 11,468", "~ 344,000", "~ 4.19 million"],
              ["Not fully vaccinated", "23.6% of births", "~ 14,871", "~ 446,000", "~ 5.43 million"],
              ["Anaemia (by 6-59 mo)", "67.1%", "~ 42,283", "~ 1.27 million", "~ 15.4 million"],
              ["Underweight (weight-for-age)", "32.1%", "~ 20,227", "~ 607,000", "~ 7.38 million"],
              ["Stunting (height-for-age)", "35.5%", "~ 22,370", "~ 671,000", "~ 8.17 million"],
              ["Wasting (weight-for-height)", "19.3%", "~ 12,162", "~ 365,000", "~ 4.44 million"],
              ["Severe wasting", "7.7%", "~ 4,852", "~ 146,000", "~ 1.77 million"],
              ["Developmental delay", "10 - 12.2%", "~ 6,301 - 7,688", "~ 189,000 - 231,000", "~ 2.30 - 2.81 million"],
              ["Vitamin D deficiency", "13.7%", "~ 8,633", "~ 259,000", "~ 3.15 million"],
              ["Cerebral palsy", "2.95 per 1,000", "~ 186", "~ 5,580", "~ 67,900"],
          ], [46 * mm, 30 * mm, 26 * mm, 32 * mm, 34 * mm], aligns=[None, None, "b", None, None]),
          Spacer(1, 4 * mm),
          callout("The daily percentage increase",
                  "Because every affected group is replenished by the same birth cohort, each grows "
                  "at almost exactly the same relative rate: <b>~ 0.055% of its standing affected "
                  "population per day</b> - about <b>1.7% per month</b> and <b>~ 20% per year</b> of "
                  "inflow. That rate is simply the birth cohort divided by the four-to-five-year "
                  "standing population (1 / ~1,680 days).",
                  TEAL_SOFT, TEAL),
          Spacer(1, 4 * mm),
          Paragraph(
              "<b>Important - this is inflow, not net growth.</b> These groups shrink as children "
              "recover, age past five, or die. India's stunting rate is genuinely falling: "
              "<b>38.4% (NFHS-4) -> 35.5% (NFHS-5) -> 29.3% (NFHS-6, 2023-24)</b>. So the standing "
              "stunted population is contracting even while roughly 22,000 newly stunted children "
              "are added every day. Wasting has barely moved (21.0% -> 19.3% -> 19.0%), and anaemia "
              "has worsened then partly recovered - the case for screening that repeats rather than "
              "a one-time measurement.", BODY),
          Spacer(1, 3 * mm),
          Paragraph("For context, India records about <b>32 under-five deaths per 1,000 live births</b> "
                    "(~ 2,017 per day), which is the only flow that removes children from these "
                    "populations at scale.", SMALL),
          PageBreak()]

# ---------------------------------------------------------------- sources
story += [Paragraph("7. Sources", H1), Spacer(1, 3 * mm)]
sources = [
    ("NFHS-5 (2019-21) India Fact Sheet", "dhsprogram.com/pubs/pdf/OF43/India_National_Fact_Sheet.pdf"),
    ("NFHS-6 (2023-24) state factsheets", "thesouthfirst.com (NFHS-5/6 compilation)"),
    ("CNNS 2016-18 - Comprehensive National Nutrition Survey", "Rana 2023, Indian Pediatrics, link.springer.com/article/10.1007/s13312-023-2835-1"),
    ("UN WPP 2024 under-5 population", "ourworldindata.org/grapher/projected-population-under-age-5"),
    ("Census of India 2011", "censusindia.gov.in"),
    ("SRS 2023 / MoSPI 'Children in India 2025'", "pib.gov.in/PressReleasePage.aspx?PRID=2171202"),
    ("UNICEF India country data", "data.unicef.org/country/ind/"),
    ("Developmental delay, community study 2024", "ijpediatrics.com"),
    ("Agarwal 2018, developmental delay", "njcmindia.com"),
    ("Lancet 2016 / Lancet Child Adolesc Health 2020 - LMIC development risk", "pmc.ncbi.nlm.nih.gov"),
    ("GBD 2016 - developmental disabilities, Olusanya, Lancet Global Health 2018", "thelancet.com/journals/langlo/article/PIIS2214-109X(18)30309-7/fulltext"),
    ("Autism India, Cureus 2024 review", "pmc.ncbi.nlm.nih.gov/articles/PMC11260197/"),
    ("Raina 2016 - autism prevalence, multi-site India", "ruralneuropractice.com"),
    ("WHO autism fact sheet (1 in 127)", "who.int/news-room/fact-sheets/detail/autism-spectrum-disorders"),
    ("Cerebral palsy, Chauhan 2019, Indian J Pediatr", "link.springer.com/article/10.1007/s12098-019-03024-0"),
    ("RBSK programme data 2014-2024", "ijcmph.com/index.php/ijcmph/article/view/15662"),
    ("RBSK / MoHFW via IMPRI", "impriindia.com"),
    ("Immunisation drop-outs, Dhalaria 2023", "pmc.ncbi.nlm.nih.gov/articles/PMC10143302/"),
    ("MUAC study, tribal Maharashtra 2021-22", "pmc.ncbi.nlm.nih.gov/articles/PMC11822597/"),
    ("Vitamin D pooled estimate 2025", "sciencedirect.com/science/article/abs/pii/S0985056225000494"),
    ("Low birth weight UNICEF-WHO 2020", "data.unicef.org/topic/nutrition/low-birthweight/"),
    ("Hearing loss screening, PMC6127065", "pmc.ncbi.nlm.nih.gov/articles/PMC6127065/"),
    ("Childhood blindness, Wadhwani 2020", "pmc.ncbi.nlm.nih.gov/articles/PMC7003592/"),
    ("Early-intervention access, Sapiets 2023", "journals.sagepub.com"),
]
for name, url in sources:
    story.append(Paragraph(f"* <b>{name}</b> - {url}", TINY))
    story.append(Spacer(1, 1.6))

story += [Spacer(1, 6 * mm),
          Paragraph("Data quality notes", H2),
          Paragraph(
              "* <b>Measured</b> means a population survey with biomarkers or a national census "
              "(NFHS, CNNS, Census). <b>Modelled</b> means a statistical estimate (UN JME, "
              "UNICEF-WHO, GBD, UN WPP). <b>Programme data</b> means administrative records "
              "(RBSK, Poshan Tracker) and reflects who was reached, not true prevalence.<br/>"
              "* India has <b>no nationally measured prevalence of developmental delay</b>. The "
              "10-12% band comes from community studies and should not be presented as a national "
              "statistic.<br/>"
              "* There is no national MUAC prevalence for India because NFHS does not collect it. "
              "The MUAC figures quoted are single-district measurements.<br/>"
              "* Absolute child counts in this document are computed from percentages applied to "
              "the UN WPP 2024 under-five population; they are planning estimates.", SMALL)]

# ---------------------------------------------------------------- build
def decorated(canvas, doc):
    canvas.saveState()
    w, h = A4
    canvas.setFillColor(TEAL)
    canvas.rect(0, h - 12 * mm, w, 12 * mm, stroke=0, fill=1)
    canvas.setFillColor(colors.white)
    canvas.setFont("Helvetica-Bold", 8.5)
    canvas.drawString(20 * mm, h - 8 * mm, "POSHAN")
    canvas.setFont("Helvetica", 8)
    canvas.drawRightString(w - 20 * mm, h - 8 * mm,
                           "India - Children 0-5 - Evidence base")
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.5)
    canvas.line(20 * mm, 14 * mm, w - 20 * mm, 14 * mm)
    canvas.setFillColor(INK_SOFT)
    canvas.setFont("Helvetica", 7.2)
    canvas.drawString(20 * mm, 10 * mm, "Poshan - Early Childhood Development & Nutrition Screening")
    canvas.drawRightString(w - 20 * mm, 10 * mm, f"Page {doc.page}")
    canvas.restoreState()


doc = BaseDocTemplate(
    OUT, pagesize=A4,
    leftMargin=20 * mm, rightMargin=20 * mm, topMargin=20 * mm, bottomMargin=20 * mm,
    title="Poshan - India Child Development & Nutrition Evidence Base",
    author="Team Poshan",
)
frame = Frame(doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="main")
doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=decorated)])
doc.build(story)
print("PDF written:", OUT, os.path.getsize(OUT) // 1024, "KB")
