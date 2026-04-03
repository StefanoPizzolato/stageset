package com.codex.stageset.chart

const val MelodyWholeNoteTicks = 128

private val SupportedMelodyLengths = setOf(1, 2, 4, 8, 16, 32)
private val SharpKeyOrder = listOf(
    MelodyStep.F,
    MelodyStep.C,
    MelodyStep.G,
    MelodyStep.D,
    MelodyStep.A,
    MelodyStep.E,
    MelodyStep.B,
)
private val FlatKeyOrder = listOf(
    MelodyStep.B,
    MelodyStep.E,
    MelodyStep.A,
    MelodyStep.D,
    MelodyStep.G,
    MelodyStep.C,
    MelodyStep.F,
)

enum class MelodyStep(
    val semitoneOffset: Int,
    val diatonicIndex: Int,
) {
    C(semitoneOffset = 0, diatonicIndex = 0),
    D(semitoneOffset = 2, diatonicIndex = 1),
    E(semitoneOffset = 4, diatonicIndex = 2),
    F(semitoneOffset = 5, diatonicIndex = 3),
    G(semitoneOffset = 7, diatonicIndex = 4),
    A(semitoneOffset = 9, diatonicIndex = 5),
    B(semitoneOffset = 11, diatonicIndex = 6),
}

enum class MelodyKeyAccidentalType(
    val symbol: String,
) {
    Sharp(smuflGlyph(0xE262)),
    Flat(smuflGlyph(0xE260)),
    Natural(""),
}

enum class MelodyClef(
    val token: String,
    val symbol: String,
    val symbolStaffStepFromBottom: Int,
    private val bottomLinePitch: MelodyReferencePitch,
    private val sharpKeySignatureSteps: IntArray,
    private val flatKeySignatureSteps: IntArray,
) {
    Treble(
        token = "treble",
        symbol = smuflGlyph(0xE050),
        symbolStaffStepFromBottom = 2,
        bottomLinePitch = MelodyReferencePitch(step = MelodyStep.E, octave = 4),
        sharpKeySignatureSteps = intArrayOf(8, 5, 9, 6, 3, 7, 4),
        flatKeySignatureSteps = intArrayOf(4, 7, 3, 6, 2, 5, 1),
    ),
    Bass(
        token = "bass",
        symbol = smuflGlyph(0xE062),
        symbolStaffStepFromBottom = 6,
        bottomLinePitch = MelodyReferencePitch(step = MelodyStep.G, octave = 2),
        sharpKeySignatureSteps = intArrayOf(6, 3, 7, 4, 1, 5, 2),
        flatKeySignatureSteps = intArrayOf(2, 5, 1, 4, 0, 3, 6),
    ),
    Alto(
        token = "alto",
        symbol = smuflGlyph(0xE05C),
        symbolStaffStepFromBottom = 4,
        bottomLinePitch = MelodyReferencePitch(step = MelodyStep.F, octave = 3),
        sharpKeySignatureSteps = intArrayOf(7, 4, 8, 5, 2, 6, 3),
        flatKeySignatureSteps = intArrayOf(3, 6, 2, 5, 1, 4, 0),
    ),
    Tenor(
        token = "tenor",
        symbol = smuflGlyph(0xE05C),
        symbolStaffStepFromBottom = 6,
        bottomLinePitch = MelodyReferencePitch(step = MelodyStep.D, octave = 3),
        sharpKeySignatureSteps = intArrayOf(5, 2, 6, 3, 0, 4, 1),
        flatKeySignatureSteps = intArrayOf(1, 4, 0, 3, 6, 2, 5),
    ),
    ;

    fun staffStepFor(pitch: MelodyPitch): Int {
        return pitch.absoluteDiatonicIndex - bottomLinePitch.absoluteDiatonicIndex
    }

    fun keySignatureStaffSteps(type: MelodyKeyAccidentalType, count: Int): List<Int> {
        val source = when (type) {
            MelodyKeyAccidentalType.Sharp -> sharpKeySignatureSteps
            MelodyKeyAccidentalType.Flat -> flatKeySignatureSteps
            MelodyKeyAccidentalType.Natural -> IntArray(0)
        }
        return source.take(count.coerceAtMost(source.size))
    }
}

data class MelodyReferencePitch(
    val step: MelodyStep,
    val octave: Int,
) {
    val absoluteDiatonicIndex: Int
        get() = (octave * 7) + step.diatonicIndex
}

data class MelodyMeter(
    val numerator: Int = 4,
    val denominator: Int = 4,
) {
    init {
        require(numerator > 0) { "Meter numerator must be positive." }
        require(denominator in SupportedMelodyLengths) {
            "Meter denominator must be one of 1, 2, 4, 8, 16, or 32."
        }
    }

    val barTicks: Int
        get() = (MelodyWholeNoteTicks * numerator) / denominator

    val label: String
        get() = "$numerator/$denominator"
}

data class MelodyKeySignature(
    val name: String = "C",
    val accidentalType: MelodyKeyAccidentalType = MelodyKeyAccidentalType.Natural,
    val accidentalCount: Int = 0,
    val accidentalsByStep: Map<MelodyStep, Int> = emptyMap(),
)

data class MelodyPitch(
    val step: MelodyStep,
    val octave: Int,
    val explicitAccidental: Int? = null,
    val keySignatureAccidental: Int = 0,
) {
    val accidental: Int
        get() = explicitAccidental ?: keySignatureAccidental

    val midiNumber: Int
        get() = ((octave + 1) * 12) + step.semitoneOffset + accidental

    val absoluteDiatonicIndex: Int
        get() = (octave * 7) + step.diatonicIndex

    val displayAccidental: Int?
        get() = explicitAccidental?.takeIf { it != keySignatureAccidental }

    val displayAccidentalSymbol: String?
        get() = when {
            explicitAccidental == 0 -> smuflGlyph(0xE261)
            displayAccidental == 1 -> smuflGlyph(0xE262)
            displayAccidental == -1 -> smuflGlyph(0xE260)
            else -> null
        }

    fun matchesPitch(other: MelodyPitch): Boolean {
        return step == other.step &&
            octave == other.octave &&
            accidental == other.accidental
    }
}

data class MelodyDuration(
    val denominator: Int,
    val dotted: Boolean = false,
) {
    init {
        require(denominator in SupportedMelodyLengths) {
            "Unsupported melody duration denominator: $denominator"
        }
    }

    val ticks: Int
        get() = (MelodyWholeNoteTicks / denominator) +
            if (dotted) (MelodyWholeNoteTicks / denominator) / 2 else 0
}

sealed interface MelodySymbol {
    val sequenceIndex: Int
    val duration: MelodyDuration
}

data class MelodyNote(
    override val sequenceIndex: Int,
    val pitch: MelodyPitch,
    override val duration: MelodyDuration,
    val tieToNext: Boolean = false,
) : MelodySymbol

data class MelodyRest(
    override val sequenceIndex: Int,
    override val duration: MelodyDuration,
) : MelodySymbol

data class MelodyBarEvent(
    val symbol: MelodySymbol,
    val startTick: Int,
)

data class MelodyBar(
    val events: List<MelodyBarEvent>,
    val totalTicks: Int,
)

data class MelodyNotation(
    val source: String,
    val tempoBpm: Int?,
    val keySignature: MelodyKeySignature,
    val meter: MelodyMeter,
    val clef: MelodyClef,
    val showKeySignature: Boolean,
    val showMeter: Boolean,
    val showClef: Boolean,
    val bars: List<MelodyBar>,
)

sealed interface MelodyParseResult {
    data class Success(val notation: MelodyNotation) : MelodyParseResult
    data class Error(val message: String) : MelodyParseResult
}

fun parseMelodyNotation(input: String): MelodyParseResult {
    return MelodyParser(input).parse()
}

private class MelodyParser(
    private val input: String,
) {
    private var index = 0
    private var currentOctave = 4
    private var currentLength = 4
    private var tempoBpm: Int? = null
    private var keySignature = defaultKeySignature()
    private var meter = MelodyMeter()
    private var clef = MelodyClef.Treble
    private var showKeySignature = false
    private var showMeter = false
    private var showClef = false
    private var parseErrorMessage: String? = null
    private val symbols = mutableListOf<MutableMelodySymbol>()
    private var pendingTieFromIndex: Int? = null

    fun parse(): MelodyParseResult {
        while (true) {
            skipWhitespace()
            if (index >= input.length) {
                break
            }

            parseErrorMessage = null
            when {
                matchesCommand("key=") -> parseKeySignature()
                    ?: return error(parseErrorMessage ?: "Unsupported key signature.")
                matchesCommand("meter=") -> parseMeter()
                    ?: return error(parseErrorMessage ?: "Meter must look like 4/4.")
                matchesCommand("cleff=") || matchesCommand("clef=") -> parseClef()
                    ?: return error(parseErrorMessage ?: "Clef must be treble, bass, alto, or tenor.")
                else -> {
                    val token = input[index].lowercaseChar()
                    if (pendingTieFromIndex != null && token !in 'a'..'g') {
                        return error("Tie must be followed by the same note.")
                    }

                    when (token) {
                        't' -> parseTempo()
                            ?: return error("Tempo must be followed by a BPM value.")
                        'o' -> parseOctave()
                            ?: return error("Octave must be between 0 and 8.")
                        'l' -> parseDefaultLength()
                            ?: return error(
                                "Default note length must be one of 1, 2, 4, 8, 16, or 32.",
                            )
                        '>' -> {
                            if ((currentOctave + 1) !in 0..8) {
                                return error("Octave must stay between 0 and 8.")
                            }
                            currentOctave += 1
                            index += 1
                        }

                        '<' -> {
                            if ((currentOctave - 1) !in 0..8) {
                                return error("Octave must stay between 0 and 8.")
                            }
                            currentOctave -= 1
                            index += 1
                        }

                        in 'a'..'g' -> parseNote(token) ?: return error(
                            parseErrorMessage ?: "Invalid note at position ${index + 1}.",
                        )
                        'r' -> parseRest() ?: return error(
                            parseErrorMessage ?: "Invalid rest at position ${index + 1}.",
                        )
                        else -> return error("Unsupported melody token '${input[index]}'.")
                    }
                }
            }
        }

        if (pendingTieFromIndex != null) {
            return error("Tie must be followed by the same note.")
        }

        if (symbols.isEmpty()) {
            return error("Melody block is empty.")
        }

        return MelodyParseResult.Success(
            notation = MelodyNotation(
                source = input.trim(),
                tempoBpm = tempoBpm,
                keySignature = keySignature,
                meter = meter,
                clef = clef,
                showKeySignature = showKeySignature,
                showMeter = showMeter,
                showClef = showClef,
                bars = buildBars(),
            ),
        )
    }

    private fun parseTempo(): Int? {
        index += 1
        val value = readNumber()?.toIntOrNull() ?: return null
        tempoBpm = value
        return value
    }

    private fun parseOctave(): Int? {
        index += 1
        val value = readNumber()?.toIntOrNull() ?: return null
        if (value !in 0..8) {
            return null
        }
        currentOctave = value
        return value
    }

    private fun parseDefaultLength(): Int? {
        index += 1
        val value = readNumber()?.toIntOrNull() ?: return null
        if (value !in SupportedMelodyLengths) {
            return null
        }
        currentLength = value
        return value
    }

    private fun parseKeySignature(): MelodyKeySignature? {
        index += "key=".length
        val keyToken = readCommandValue()
        val parsed = parseKeySignatureToken(keyToken)
        if (parsed == null) {
            parseErrorMessage = "Unsupported key signature '$keyToken'."
            return null
        }
        keySignature = parsed
        showKeySignature = true
        return parsed
    }

    private fun parseMeter(): MelodyMeter? {
        index += "meter=".length
        val meterToken = readCommandValue()
        val parts = meterToken.split('/')
        if (parts.size != 2) {
            parseErrorMessage = "Meter must look like 4/4."
            return null
        }

        val numerator = parts[0].toIntOrNull()
        val denominator = parts[1].toIntOrNull()
        if (numerator == null || denominator == null) {
            parseErrorMessage = "Meter must look like 4/4."
            return null
        }
        if (numerator <= 0 || denominator !in SupportedMelodyLengths) {
            parseErrorMessage = "Meter denominator must be one of 1, 2, 4, 8, 16, or 32."
            return null
        }

        meter = MelodyMeter(
            numerator = numerator,
            denominator = denominator,
        )
        showMeter = true
        return meter
    }

    private fun parseClef(): MelodyClef? {
        index += if (matchesCommand("cleff=")) "cleff=".length else "clef=".length
        val clefToken = readCommandValue().lowercase()
        val parsed = MelodyClef.entries.firstOrNull { it.token == clefToken }
        if (parsed == null) {
            parseErrorMessage = "Clef must be treble, bass, alto, or tenor."
            return null
        }
        clef = parsed
        showClef = true
        return parsed
    }

    private fun parseNote(stepToken: Char): MelodyNote? {
        index += 1
        val explicitAccidental = parseAccidental()
        val duration = parseDuration() ?: return null
        val note = MutableMelodyNote(
            sequenceIndex = symbols.size,
            pitch = MelodyPitch(
                step = stepFor(stepToken),
                octave = currentOctave,
                explicitAccidental = explicitAccidental,
                keySignatureAccidental = keySignature.accidentalsByStep[stepFor(stepToken)] ?: 0,
            ),
            duration = duration,
        )

        val tieError = linkPendingTieIfNeeded(note)
        if (tieError != null) {
            parseErrorMessage = tieError
            return null
        }

        symbols += note
        captureTieContinuation(fromIndex = note.sequenceIndex)
        return note.toImmutable()
    }

    private fun parseRest(): MelodyRest? {
        if (pendingTieFromIndex != null) {
            parseErrorMessage = "Tie must join notes."
            return null
        }

        index += 1
        val duration = parseDuration() ?: return null
        val rest = MutableMelodyRest(
            sequenceIndex = symbols.size,
            duration = duration,
        )
        symbols += rest
        skipWhitespace()
        if (peekChar() == '&') {
            parseErrorMessage = "Tie must join notes."
            return null
        }
        return rest.toImmutable()
    }

    private fun parseAccidental(): Int? {
        return when (peekChar()) {
            '#', '+' -> {
                index += 1
                1
            }

            '-', 'b', 'B' -> {
                index += 1
                -1
            }

            'n', 'N' -> {
                index += 1
                0
            }

            else -> null
        }
    }

    private fun parseDuration(): MelodyDuration? {
        val explicitLength = readNumber()?.toIntOrNull()
        val denominator = explicitLength ?: currentLength
        if (denominator !in SupportedMelodyLengths) {
            parseErrorMessage = "Note length must be one of 1, 2, 4, 8, 16, or 32."
            return null
        }
        if (explicitLength != null) {
            currentLength = explicitLength
        }

        val dotted = if (peekChar() == '.') {
            index += 1
            true
        } else {
            false
        }

        return MelodyDuration(
            denominator = denominator,
            dotted = dotted,
        )
    }

    private fun linkPendingTieIfNeeded(nextNote: MutableMelodyNote): String? {
        val previousIndex = pendingTieFromIndex ?: return null
        val previous = symbols.getOrNull(previousIndex) as? MutableMelodyNote ?: return "Tie must join notes."
        if (!previous.pitch.matchesPitch(nextNote.pitch)) {
            return "Tie must join the same pitch."
        }
        previous.tieToNext = true
        pendingTieFromIndex = null
        return null
    }

    private fun captureTieContinuation(fromIndex: Int) {
        skipWhitespace()
        if (peekChar() == '&') {
            pendingTieFromIndex = fromIndex
            index += 1
        }
    }

    private fun buildBars(): List<MelodyBar> {
        val bars = mutableListOf<MelodyBar>()
        var currentEvents = mutableListOf<MelodyBarEvent>()
        var currentTicks = 0
        val barTicks = meter.barTicks

        symbols.forEach { symbol ->
            val immutableSymbol = symbol.toImmutable()
            val symbolTicks = immutableSymbol.duration.ticks

            if (currentEvents.isNotEmpty() && currentTicks + symbolTicks > barTicks) {
                bars += MelodyBar(
                    events = currentEvents.toList(),
                    totalTicks = currentTicks,
                )
                currentEvents = mutableListOf()
                currentTicks = 0
            }

            currentEvents += MelodyBarEvent(
                symbol = immutableSymbol,
                startTick = currentTicks,
            )
            currentTicks += symbolTicks

            if (currentTicks >= barTicks) {
                bars += MelodyBar(
                    events = currentEvents.toList(),
                    totalTicks = currentTicks,
                )
                currentEvents = mutableListOf()
                currentTicks = 0
            }
        }

        if (currentEvents.isNotEmpty()) {
            bars += MelodyBar(
                events = currentEvents.toList(),
                totalTicks = currentTicks,
            )
        }

        return bars
    }

    private fun matchesCommand(command: String): Boolean {
        return input.regionMatches(index, command, 0, command.length, ignoreCase = true)
    }

    private fun skipWhitespace() {
        while (index < input.length && input[index].isWhitespace()) {
            index += 1
        }
    }

    private fun readNumber(): String? {
        val start = index
        while (index < input.length && input[index].isDigit()) {
            index += 1
        }
        return if (index > start) {
            input.substring(start, index)
        } else {
            null
        }
    }

    private fun readCommandValue(): String {
        val start = index
        while (index < input.length && !input[index].isWhitespace()) {
            index += 1
        }
        return input.substring(start, index).trim()
    }

    private fun peekChar(): Char? = input.getOrNull(index)

    private fun error(message: String): MelodyParseResult.Error {
        return MelodyParseResult.Error(message = message)
    }

    private fun stepFor(token: Char): MelodyStep {
        return when (token.lowercaseChar()) {
            'c' -> MelodyStep.C
            'd' -> MelodyStep.D
            'e' -> MelodyStep.E
            'f' -> MelodyStep.F
            'g' -> MelodyStep.G
            'a' -> MelodyStep.A
            'b' -> MelodyStep.B
            else -> throw IllegalArgumentException("Unsupported step token '$token'.")
        }
    }
}

private sealed interface MutableMelodySymbol {
    val sequenceIndex: Int
    val duration: MelodyDuration

    fun toImmutable(): MelodySymbol
}

private data class MutableMelodyNote(
    override val sequenceIndex: Int,
    val pitch: MelodyPitch,
    override val duration: MelodyDuration,
    var tieToNext: Boolean = false,
) : MutableMelodySymbol {
    override fun toImmutable(): MelodyNote {
        return MelodyNote(
            sequenceIndex = sequenceIndex,
            pitch = pitch,
            duration = duration,
            tieToNext = tieToNext,
        )
    }
}

private data class MutableMelodyRest(
    override val sequenceIndex: Int,
    override val duration: MelodyDuration,
) : MutableMelodySymbol {
    override fun toImmutable(): MelodyRest {
        return MelodyRest(
            sequenceIndex = sequenceIndex,
            duration = duration,
        )
    }
}

private fun defaultKeySignature(): MelodyKeySignature {
    return MelodyKeySignature()
}

private fun parseKeySignatureToken(token: String): MelodyKeySignature? {
    return KeySignatureLookup[token.lowercase()]
}

private val KeySignatureLookup = buildMap {
    addKey("C", MelodyKeyAccidentalType.Natural, 0)
    addKey("G", MelodyKeyAccidentalType.Sharp, 1)
    addKey("D", MelodyKeyAccidentalType.Sharp, 2)
    addKey("A", MelodyKeyAccidentalType.Sharp, 3)
    addKey("E", MelodyKeyAccidentalType.Sharp, 4)
    addKey("B", MelodyKeyAccidentalType.Sharp, 5)
    addKey("F#", MelodyKeyAccidentalType.Sharp, 6)
    addKey("C#", MelodyKeyAccidentalType.Sharp, 7)
    addKey("F", MelodyKeyAccidentalType.Flat, 1)
    addKey("Bb", MelodyKeyAccidentalType.Flat, 2)
    addKey("Eb", MelodyKeyAccidentalType.Flat, 3)
    addKey("Ab", MelodyKeyAccidentalType.Flat, 4)
    addKey("Db", MelodyKeyAccidentalType.Flat, 5)
    addKey("Gb", MelodyKeyAccidentalType.Flat, 6)
    addKey("Cb", MelodyKeyAccidentalType.Flat, 7)

    addKey("Am", MelodyKeyAccidentalType.Natural, 0)
    addKey("Em", MelodyKeyAccidentalType.Sharp, 1)
    addKey("Bm", MelodyKeyAccidentalType.Sharp, 2)
    addKey("F#m", MelodyKeyAccidentalType.Sharp, 3)
    addKey("C#m", MelodyKeyAccidentalType.Sharp, 4)
    addKey("G#m", MelodyKeyAccidentalType.Sharp, 5)
    addKey("D#m", MelodyKeyAccidentalType.Sharp, 6)
    addKey("A#m", MelodyKeyAccidentalType.Sharp, 7)
    addKey("Dm", MelodyKeyAccidentalType.Flat, 1)
    addKey("Gm", MelodyKeyAccidentalType.Flat, 2)
    addKey("Cm", MelodyKeyAccidentalType.Flat, 3)
    addKey("Fm", MelodyKeyAccidentalType.Flat, 4)
    addKey("Bbm", MelodyKeyAccidentalType.Flat, 5)
    addKey("Ebm", MelodyKeyAccidentalType.Flat, 6)
    addKey("Abm", MelodyKeyAccidentalType.Flat, 7)
}

private fun MutableMap<String, MelodyKeySignature>.addKey(
    name: String,
    accidentalType: MelodyKeyAccidentalType,
    accidentalCount: Int,
) {
    val accidentalsByStep = when (accidentalType) {
        MelodyKeyAccidentalType.Sharp -> SharpKeyOrder.take(accidentalCount).associateWith { 1 }
        MelodyKeyAccidentalType.Flat -> FlatKeyOrder.take(accidentalCount).associateWith { -1 }
        MelodyKeyAccidentalType.Natural -> emptyMap()
    }

    put(
        name.lowercase(),
        MelodyKeySignature(
            name = name,
            accidentalType = accidentalType,
            accidentalCount = accidentalCount,
            accidentalsByStep = accidentalsByStep,
        ),
    )
}

private fun smuflGlyph(codePoint: Int): String {
    return String(Character.toChars(codePoint))
}
