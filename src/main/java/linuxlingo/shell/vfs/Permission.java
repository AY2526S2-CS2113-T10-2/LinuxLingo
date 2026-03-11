package linuxlingo.shell.vfs;

/**
 * Permission model for VFS nodes.
 * Represents Unix-like 9-character permission string (e.g., "rwxr-xr-x").
 */
public class Permission {
    private final boolean[] bits; // length 9

    public Permission(String permString) {
        if (permString.length() != 9) {
            throw new IllegalArgumentException("Permission string must be 9 characters: " + permString);
        }
        bits = new boolean[9];
        char[] expected = {'r', 'w', 'x', 'r', 'w', 'x', 'r', 'w', 'x'};
        for (int i = 0; i < 9; i++) {
            char c = permString.charAt(i);
            if (c == expected[i] || (i == 8 && c == 't')) {
                bits[i] = true;
            } else if (c == '-' || (i == 8 && c == 'T')) {
                bits[i] = (c == 'T' || c == 't'); // sticky bit edge case
                if (c == '-') {
                    bits[i] = false;
                }
            } else if (c == 's' || c == 'S') {
                bits[i] = (c == 's');
            } else {
                bits[i] = false;
            }
        }
    }

    private Permission(boolean[] bits) {
        this.bits = bits.clone();
    }

    public static Permission fromOctal(String octal) {
        if (octal.length() != 3) {
            throw new IllegalArgumentException("Octal permission must be 3 digits: " + octal);
        }
        boolean[] b = new boolean[9];
        for (int i = 0; i < 3; i++) {
            int digit = octal.charAt(i) - '0';
            if (digit < 0 || digit > 7) {
                throw new IllegalArgumentException("Invalid octal digit: " + octal.charAt(i));
            }
            b[i * 3] = (digit & 4) != 0;
            b[i * 3 + 1] = (digit & 2) != 0;
            b[i * 3 + 2] = (digit & 1) != 0;
        }
        return new Permission(b);
    }

    public static Permission fromSymbolic(String symbolic, Permission current) {
        boolean[] b = current.bits.clone();
        // Parse e.g. "u+x", "g-w", "o+rw", "a+x", "u=rwx"
        int i = 0;
        while (i < symbolic.length()) {
            // Parse who
            boolean user = false;
            boolean group = false;
            boolean others = false;
            while (i < symbolic.length() && "ugoa".indexOf(symbolic.charAt(i)) >= 0) {
                switch (symbolic.charAt(i)) {
                case 'u' -> user = true;
                case 'g' -> group = true;
                case 'o' -> others = true;
                case 'a' -> {
                    user = true;
                    group = true;
                    others = true;
                }
                default -> { }
                }
                i++;
            }
            if (!user && !group && !others) {
                user = true;
                group = true;
                others = true;
            }
            if (i >= symbolic.length()) {
                break;
            }
            char op = symbolic.charAt(i++);
            // Parse permissions
            boolean r = false;
            boolean w = false;
            boolean x = false;
            while (i < symbolic.length() && "rwx".indexOf(symbolic.charAt(i)) >= 0) {
                switch (symbolic.charAt(i)) {
                case 'r' -> r = true;
                case 'w' -> w = true;
                case 'x' -> x = true;
                default -> { }
                }
                i++;
            }
            // Apply
            applyPermissionOp(b, op, user, group, others, r, w, x);
            // Skip comma separators
            if (i < symbolic.length() && symbolic.charAt(i) == ',') {
                i++;
            }
        }
        return new Permission(b);
    }

    // CHECKSTYLE.OFF: OverloadMethodsDeclarationOrder
    private static void applyPermissionOp(boolean[] b, char op,
            boolean user, boolean group, boolean others,
            boolean r, boolean w, boolean x) {
        switch (op) {
        case '+' -> {
            applyAdd(b, user, group, others, r, w, x);
        }
        case '-' -> {
            applyRemove(b, user, group, others, r, w, x);
        }
        case '=' -> {
            applySet(b, user, group, others, r, w, x);
        }
        default -> { }
        }
    }

    private static void applyAdd(boolean[] b,
            boolean user, boolean group, boolean others,
            boolean r, boolean w, boolean x) {
        if (user) {
            if (r) {
                b[0] = true;
            }
            if (w) {
                b[1] = true;
            }
            if (x) {
                b[2] = true;
            }
        }
        if (group) {
            if (r) {
                b[3] = true;
            }
            if (w) {
                b[4] = true;
            }
            if (x) {
                b[5] = true;
            }
        }
        if (others) {
            if (r) {
                b[6] = true;
            }
            if (w) {
                b[7] = true;
            }
            if (x) {
                b[8] = true;
            }
        }
    }

    private static void applyRemove(boolean[] b,
            boolean user, boolean group, boolean others,
            boolean r, boolean w, boolean x) {
        if (user) {
            if (r) {
                b[0] = false;
            }
            if (w) {
                b[1] = false;
            }
            if (x) {
                b[2] = false;
            }
        }
        if (group) {
            if (r) {
                b[3] = false;
            }
            if (w) {
                b[4] = false;
            }
            if (x) {
                b[5] = false;
            }
        }
        if (others) {
            if (r) {
                b[6] = false;
            }
            if (w) {
                b[7] = false;
            }
            if (x) {
                b[8] = false;
            }
        }
    }

    private static void applySet(boolean[] b,
            boolean user, boolean group, boolean others,
            boolean r, boolean w, boolean x) {
        if (user) {
            b[0] = r;
            b[1] = w;
            b[2] = x;
        }
        if (group) {
            b[3] = r;
            b[4] = w;
            b[5] = x;
        }
        if (others) {
            b[6] = r;
            b[7] = w;
            b[8] = x;
        }
    }
    // CHECKSTYLE.ON: OverloadMethodsDeclarationOrder

    public boolean canOwnerRead() {
        return bits[0];
    }

    public boolean canOwnerWrite() {
        return bits[1];
    }

    public boolean canOwnerExecute() {
        return bits[2];
    }

    public boolean canGroupRead() {
        return bits[3];
    }

    public boolean canGroupWrite() {
        return bits[4];
    }

    public boolean canGroupExecute() {
        return bits[5];
    }

    public boolean canOtherRead() {
        return bits[6];
    }

    public boolean canOtherWrite() {
        return bits[7];
    }

    public boolean canOtherExecute() {
        return bits[8];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(9);
        sb.append(bits[0] ? 'r' : '-');
        sb.append(bits[1] ? 'w' : '-');
        sb.append(bits[2] ? 'x' : '-');
        sb.append(bits[3] ? 'r' : '-');
        sb.append(bits[4] ? 'w' : '-');
        sb.append(bits[5] ? 'x' : '-');
        sb.append(bits[6] ? 'r' : '-');
        sb.append(bits[7] ? 'w' : '-');
        sb.append(bits[8] ? 'x' : '-');
        return sb.toString();
    }

    public String toOctal() {
        int owner = (bits[0] ? 4 : 0) + (bits[1] ? 2 : 0) + (bits[2] ? 1 : 0);
        int group = (bits[3] ? 4 : 0) + (bits[4] ? 2 : 0) + (bits[5] ? 1 : 0);
        int other = (bits[6] ? 4 : 0) + (bits[7] ? 2 : 0) + (bits[8] ? 1 : 0);
        return "" + owner + group + other;
    }

    public Permission copy() {
        return new Permission(bits);
    }
}
