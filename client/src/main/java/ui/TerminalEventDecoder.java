package ui;

/** Decodes xterm SGR and legacy mouse reports without mixing them into typed commands. */
final class TerminalEventDecoder {
    enum Key { TEXT, ENTER, BACKSPACE, LEFT, RIGHT, HOME, END, DELETE, CANCEL, EXIT, MOUSE }
    record Event(Key key, int character, int x, int y, boolean rightButton) {
        static Event key(Key key) {
            return new Event(key, 0, 0, 0, false);
        }
    }

    private final StringBuilder escape = new StringBuilder();

    Event feed(int character) {
        if (!escape.isEmpty()) {
            escape.append((char) character);
            String sequence = escape.toString();
            if (sequence.startsWith("\u001b[M")) {
                if (escape.length() < 6) {
                    return null;
                }
                escape.setLength(0);
                return mouse(sequence.charAt(3) - 32, sequence.charAt(4) - 33,
                        sequence.charAt(5) - 33, false);
            }
            if (escape.length() == 2 && (character == '[' || character == 'O')) {
                return null;
            }
            if (escape.length() >= 64 || (escape.length() == 2 && character != '[' && character != 'O')) {
                escape.setLength(0);
                return null;
            }
            if (sequence.startsWith("\u001b[<") && character != 'M' && character != 'm') {
                return null;
            }
            if (escape.length() >= 3 && character >= 0x40 && character <= 0x7e) {
                escape.setLength(0);
                if (sequence.startsWith("\u001b[<")) {
                    return sgrMouse(sequence);
                }
                return switch (sequence) {
                    case "\u001b[D", "\u001bOD" -> Event.key(Key.LEFT);
                    case "\u001b[C", "\u001bOC" -> Event.key(Key.RIGHT);
                    case "\u001b[H", "\u001bOH", "\u001b[1~" -> Event.key(Key.HOME);
                    case "\u001b[F", "\u001bOF", "\u001b[4~" -> Event.key(Key.END);
                    case "\u001b[3~" -> Event.key(Key.DELETE);
                    default -> null;
                };
            }
            return null;
        }
        return switch (character) {
            case 27 -> {
                escape.append((char) character);
                yield null;
            }
            case 3, 4, -1 -> Event.key(Key.EXIT);
            case '\n', '\r' -> Event.key(Key.ENTER);
            case 8, 127 -> Event.key(Key.BACKSPACE);
            case 21 -> Event.key(Key.CANCEL); // Ctrl-U
            default -> character >= 32 ? new Event(Key.TEXT, character, 0, 0, false) : null;
        };
    }

    Event idle() {
        if (escape.toString().equals("\u001b")) {
            escape.setLength(0);
            return Event.key(Key.CANCEL);
        }
        return null;
    }

    private static Event sgrMouse(String sequence) {
        if (!sequence.endsWith("M") && !sequence.endsWith("m")) {
            return null;
        }
        String[] parts = sequence.substring(3, sequence.length() - 1).split(";", -1);
        if (parts.length != 3) {
            return null;
        }
        try {
            return mouse(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]) - 1, sequence.endsWith("m"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Event mouse(int button, int x, int y, boolean release) {
        // Wheel, drag, modifier-click, middle click and releases never submit moves.
        if (release || button < 0 || button > 2 || button == 1 || x < 0 || y < 0) {
            return null;
        }
        return new Event(Key.MOUSE, 0, x, y, button == 2);
    }
}
