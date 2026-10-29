package hu.nutrinaplo.app.core

enum class NutrientGroup(val label: String) {
    MACRO("Makrotápanyagok"),
    VITAMIN("Vitaminok"),
    MINERAL("Ásványi anyagok"),
}

data class NutrientDef(
    val key: String,
    val label: String,
    val unit: String,
    val group: NutrientGroup,
    val decimals: Int = 1,
    /** rövid leírás: mire jó, miben van */
    val info: String = "",
)

object Nutrients {
    const val KCAL = "kcal"
    const val PROTEIN = "protein"
    const val CARBS = "carbs"
    const val FAT = "fat"
    const val FIBER = "fiber"
    const val SUGAR = "sugar"
    const val SATFAT = "satfat"
    const val SODIUM = "na"

    val all: List<NutrientDef> = listOf(
        NutrientDef(KCAL, "Energia", "kcal", NutrientGroup.MACRO, 0),
        NutrientDef(PROTEIN, "Fehérje", "g", NutrientGroup.MACRO),
        NutrientDef(CARBS, "Szénhidrát", "g", NutrientGroup.MACRO),
        NutrientDef(SUGAR, "ebből cukor", "g", NutrientGroup.MACRO),
        NutrientDef(FIBER, "Élelmi rost", "g", NutrientGroup.MACRO),
        NutrientDef(FAT, "Zsír", "g", NutrientGroup.MACRO),
        NutrientDef(SATFAT, "ebből telített zsírsav", "g", NutrientGroup.MACRO),
        NutrientDef("mufa", "Egyszeresen telítetlen zsírsav", "g", NutrientGroup.MACRO),
        NutrientDef("pufa", "Többszörösen telítetlen zsírsav", "g", NutrientGroup.MACRO),
        NutrientDef("chol", "Koleszterin", "mg", NutrientGroup.MACRO, 0),

        NutrientDef("vita", "A-vitamin", "µg", NutrientGroup.VITAMIN, 0,
            "Látás, immunrendszer, bőr. Forrás: máj, sárgarépa, édesburgonya, spenót, tojás."),
        NutrientDef("vitc", "C-vitamin", "mg", NutrientGroup.VITAMIN, 0,
            "Immunrendszer, kollagén, vasfelszívódás. Forrás: paprika, citrusok, kivi, brokkoli."),
        NutrientDef("vitd", "D-vitamin", "µg", NutrientGroup.VITAMIN, 1,
            "Csontok, immunrendszer. Forrás: zsíros halak, tojássárgája, napfény. Télen gyakran pótolni kell."),
        NutrientDef("vite", "E-vitamin", "mg", NutrientGroup.VITAMIN, 1,
            "Antioxidáns. Forrás: napraforgómag, mandula, növényi olajok, mogyoró."),
        NutrientDef("vitk", "K-vitamin", "µg", NutrientGroup.VITAMIN, 0,
            "Véralvadás, csontok. Forrás: leveles zöldségek, brokkoli, kelbimbó."),
        NutrientDef("b1", "B1-vitamin (tiamin)", "mg", NutrientGroup.VITAMIN, 2,
            "Energia-anyagcsere, idegrendszer. Forrás: sertéshús, teljes kiőrlésű gabonák, hüvelyesek."),
        NutrientDef("b2", "B2-vitamin (riboflavin)", "mg", NutrientGroup.VITAMIN, 2,
            "Energia-anyagcsere. Forrás: tej, tojás, máj, gomba, mandula."),
        NutrientDef("b3", "B3-vitamin (niacin)", "mg", NutrientGroup.VITAMIN, 1,
            "Energia-anyagcsere, bőr. Forrás: hús, hal, földimogyoró, gomba."),
        NutrientDef("b5", "B5-vitamin (pantoténsav)", "mg", NutrientGroup.VITAMIN, 1,
            "Energia-anyagcsere. Forrás: máj, tojás, gomba, avokádó, csirke."),
        NutrientDef("b6", "B6-vitamin", "mg", NutrientGroup.VITAMIN, 2,
            "Fehérje-anyagcsere, idegrendszer. Forrás: csirke, hal, burgonya, banán, csicseriborsó."),
        NutrientDef("dfe", "B9-vitamin (folát)", "µg", NutrientGroup.VITAMIN, 0,
            "Vérképzés, sejtosztódás. Forrás: spenót, hüvelyesek, spárga, máj, brokkoli."),
        NutrientDef("b12", "B12-vitamin", "µg", NutrientGroup.VITAMIN, 1,
            "Vérképzés, idegrendszer. Csak állati eredetű ételekben: hús, hal, tojás, tej."),
        NutrientDef("choline", "Kolin", "mg", NutrientGroup.VITAMIN, 0,
            "Máj, idegrendszer. Forrás: tojás, máj, hús, hal."),

        NutrientDef("ca", "Kalcium", "mg", NutrientGroup.MINERAL, 0,
            "Csontok, fogak, izomműködés. Forrás: tej, sajt, joghurt, mák, szezám."),
        NutrientDef("fe", "Vas", "mg", NutrientGroup.MINERAL, 1,
            "Oxigénszállítás. Forrás: vörös hús, máj, hüvelyesek, spenót, tökmag."),
        NutrientDef("mg", "Magnézium", "mg", NutrientGroup.MINERAL, 0,
            "Izom- és idegműködés. Forrás: tökmag, mandula, étcsokoládé, hüvelyesek, zab."),
        NutrientDef("p", "Foszfor", "mg", NutrientGroup.MINERAL, 0,
            "Csontok, energia. Forrás: hús, hal, tejtermékek, magvak."),
        NutrientDef("k", "Kálium", "mg", NutrientGroup.MINERAL, 0,
            "Vérnyomás, izomműködés. Forrás: burgonya, banán, bab, spenót, avokádó."),
        NutrientDef(SODIUM, "Nátrium", "mg", NutrientGroup.MINERAL, 0,
            "Főleg a sóból. Túl sok emeli a vérnyomást (1 g só ≈ 400 mg nátrium)."),
        NutrientDef("zn", "Cink", "mg", NutrientGroup.MINERAL, 1,
            "Immunrendszer, sebgyógyulás. Forrás: marhahús, tökmag, sajt, csicseriborsó."),
        NutrientDef("cu", "Réz", "mg", NutrientGroup.MINERAL, 2,
            "Vérképzés. Forrás: máj, kesudió, étcsokoládé, magvak."),
        NutrientDef("mn", "Mangán", "mg", NutrientGroup.MINERAL, 2,
            "Anyagcsere. Forrás: teljes kiőrlésű gabonák, diófélék, tea."),
        NutrientDef("se", "Szelén", "µg", NutrientGroup.MINERAL, 0,
            "Antioxidáns, pajzsmirigy. Forrás: brazil dió, hal, tojás, hús."),
    )

    val byKey: Map<String, NutrientDef> = all.associateBy { it.key }
    val micros: List<NutrientDef> = all.filter { it.group != NutrientGroup.MACRO }
    val vitamins: List<NutrientDef> = all.filter { it.group == NutrientGroup.VITAMIN }
    val minerals: List<NutrientDef> = all.filter { it.group == NutrientGroup.MINERAL }

    fun label(key: String): String = byKey[key]?.label ?: key
    fun unit(key: String): String = byKey[key]?.unit ?: ""
}
