package com.lambda.app.data

/** Default HSC starter syllabus (ported 1:1 from the web app). */
private class RawGroup(val name: String, val cols: List<String>, val topics: List<String>)
private class RawSubject(val name: String, val icon: String, val groups: List<RawGroup>)

private val RAW_SYLLABUS = listOf(
    RawSubject("Bangla", "📖", listOf(
        RawGroup("গদ্য (1st Paper)", listOf("Class", "Test Paper"), listOf(
            "বাঙ্গালার নব্য লেখকদিগের প্রতি নিবেদন",
            "অপরিচিতা",
            "সাহিত্যে খেলা",
            "বিলাসী",
            "অর্ধাঙ্গী",
            "যৌবনের গান",
            "জীবন ও বৃক্ষ",
            "গন্তব্য কাবুল",
            "মাসি-পিসি",
            "রেইনকোট",
            "নেকলেস",
            "কপিলদাস মুর্মুর শেষ কাজ"
        )),
        RawGroup("পদ্য (2nd Paper)", listOf("Class", "Test Paper"), listOf(
            "ঋতু-বর্ণন",
            "বিভীষণের প্রতি মেঘনাদ",
            "সোনার তরী",
            "বিদ্রোহী",
            "সুচেতনা",
            "প্রতিদান",
            "তাহারেই পড়ে মনে",
            "পদ্মা",
            "ফেব্রুয়ারি ১৯৬৯",
            "আমি কিংবদন্তির কথা বলছি",
            "প্রত্যাবর্তনের লজ্জা"
        )),
        RawGroup("সহপাঠ (3rd Component)", listOf("Class", "Test Paper"), listOf(
            "সিরাজউদ্দৌলা (নাটক)",
            "লালসালু (উপন্যাস)"
        ))
    )),
    RawSubject("Physics", "⚛", listOf(
        RawGroup("1st Paper", listOf("Class", "Short Notes", "Engineering", "Test Paper"), listOf(
            "ভৌত জগৎ ও পরিমাপ",
            "ভেক্টর",
            "গতি",
            "নিউটনীয় বলবিদ্যা",
            "কাজ, ক্ষমতা ও শক্তি",
            "মহাকর্ষ ও অভিকর্ষ",
            "পদার্থের গাঠনিক ধর্ম",
            "পর্যাবৃত্ত গতি",
            "তরঙ্গ",
            "আদর্শ গ্যাস ও গ্যাসের গতিতত্ত্ব"
        )),
        RawGroup("2nd Paper", listOf("Class", "Short Notes", "Engineering", "Test Paper"), listOf(
            "তাপগতিবিদ্যা",
            "স্থির তড়িৎ",
            "চল তড়িৎ",
            "তড়িৎ প্রবাহের চৌম্বক ক্রিয়া",
            "তড়িৎচৌম্বকীয় আবেশ ও পরিবর্তী প্রবাহ",
            "জ্যামিতিক আলোকবিজ্ঞান",
            "ভৌত আলোকবিজ্ঞান",
            "আধুনিক পদার্থবিজ্ঞানের সূচনা",
            "পরমাণুর মডেল ও নিউক্লিয়ার পদার্থবিজ্ঞান",
            "সেমিকন্ডাক্টর ও ইলেকট্রনিক্স",
            "জ্যোতির্বিজ্ঞান"
        ))
    )),
    RawSubject("Math", "∑", listOf(
        RawGroup("Higher Math 1st Paper", listOf("Class", "Short Notes", "Engineering", "Test Paper"), listOf(
            "ম্যাট্রিক্স ও নির্ণায়ক",
            "ভেক্টর",
            "সরলরেখা",
            "বৃত্ত",
            "বিন্যাস ও সমাবেশ",
            "ত্রিকোণমিতিক অনুপাত",
            "সংযুক্ত কোণের ত্রিকোণমিতিক অনুপাত",
            "ফাংশন ও ফাংশনের লেখচিত্র",
            "অন্তরীকরণ",
            "যোগজীকরণ"
        )),
        RawGroup("Higher Math 2nd Paper", listOf("Class", "Short Notes", "Engineering", "Test Paper"), listOf(
            "বাস্তব সংখ্যা ও অসমতা",
            "যোগাশ্রয়ী প্রোগ্রাম",
            "জটিল সংখ্যা",
            "বহুপদী ও বহুপদী সমীকরণ",
            "দ্বিপদী বিস্তৃতি",
            "কনিক",
            "বিপরীত ত্রিকোণমিতিক ফাংশন ও সমীকরণ",
            "স্থিতিবিদ্যা",
            "সমতলে বস্তুকণার গতি",
            "বিস্তার পরিমাপ ও সম্ভাবনা"
        ))
    )),
    RawSubject("Chemistry", "⚗", listOf(
        RawGroup("1st Paper", listOf("Class", "Short Notes", "Engineering", "Test Paper"), listOf(
            "ল্যাবরেটরির নিরাপদ ব্যবহার",
            "গুণগত রসায়ন",
            "মৌলের পর্যায়বৃত্ত ধর্ম ও রাসায়নিক বন্ধন",
            "রাসায়নিক পরিবর্তন",
            "কর্মমুখী রসায়ন"
        )),
        RawGroup("2nd Paper", listOf("Class", "Short Notes", "Engineering", "Test Paper"), listOf(
            "পরিবেশ রসায়ন",
            "জৈব রসায়ন",
            "পরিমাণগত রসায়ন",
            "তড়িৎ রসায়ন",
            "অর্থনৈতিক রসায়ন"
        ))
    )),
    RawSubject("Botany", "🌿", listOf(
        RawGroup("উদ্ভিদবিজ্ঞান", listOf("Class", "Test Paper"), listOf(
            "কোষ ও এর গঠন",
            "কোষ বিভাজন",
            "কোষ রসায়ন",
            "অণুজীব",
            "শৈবাল ও ছত্রাক",
            "ব্রায়োফাইটা ও টেরিডোফাইটা",
            "নগ্নবীজী ও আবৃতবীজী উদ্ভিদ",
            "টিস্যু ও টিস্যুতন্ত্র",
            "উদ্ভিদ শারীরতত্ত্ব",
            "উদ্ভিদ প্রজনন",
            "জীবপ্রযুক্তি",
            "জীবের পরিবেশ"
        ))
    )),
    RawSubject("Zoology", "🐚", listOf(
        RawGroup("প্রাণীবিজ্ঞান", listOf("Class", "Test Paper"), listOf(
            "প্রাণীর বিভিন্নতা ও শ্রেণিবিন্যাস",
            "প্রাণীর পরিচিতি (হাইড্রা)",
            "পরিপাক ও শোষণ",
            "রক্ত ও সঞ্চালন",
            "শ্বাসক্রিয়া",
            "বর্জ্য ও নিষ্কাশন",
            "চলন ও অঙ্গচালনা",
            "সমন্বয় ও নিয়ন্ত্রণ",
            "মানব জীবনের ধারাবাহিকতা",
            "মানবদেহের প্রতিরক্ষা",
            "জিনতত্ত্ব ও বিবর্তন",
            "প্রাণীর আচরণ"
        ))
    )),
    RawSubject("ICT", "💻", listOf(
        RawGroup("তথ্য ও যোগাযোগ প্রযুক্তি", listOf("Class", "Test Paper"), listOf(
            "তথ্য ও যোগাযোগ প্রযুক্তি : বিশ্ব ও বাংলাদেশ প্রেক্ষিত",
            "কমিউনিকেশন সিস্টেমস ও নেটওয়ার্কিং",
            "সংখ্যা পদ্ধতি ও ডিজিটাল ডিভাইস",
            "ওয়েব ডিজাইন পরিচিতি ও HTML",
            "প্রোগ্রামিং ভাষা (C)",
            "ডেটাবেজ ম্যানেজমেন্ট সিস্টেম"
        ))
    )),
    RawSubject("English 2nd Paper", "✒", listOf(
        RawGroup("Grammar & Composition", listOf("Class", "Test Paper", "Question Bank"), listOf(
            "Articles",
            "Preposition",
            "Phrase",
            "Right Form of Verb",
            "Transformation",
            "Narration",
            "Pronoun",
            "Modifiers",
            "Connectors",
            "Synonym / Antonym",
            "Punctuation"
        ))
    ))
)

fun createDefaultSubjects(): List<Subject> =
    RAW_SYLLABUS.mapIndexed { si, s ->
        Subject(
            id = "subj-$si",
            name = s.name,
            icon = s.icon,
            groups = s.groups.mapIndexed { gi, g ->
                SubjectGroup(
                    id = "subj-$si-g$gi",
                    name = g.name,
                    columns = g.cols,
                    chapters = g.topics.mapIndexed { ci, c ->
                        Chapter(id = "subj-$si-g$gi-c$ci", name = c)
                    }
                )
            }
        )
    }
