package org.totipo.desktop.ui;

import org.totipo.TokenDescriptor;

/** Non-secret display shared by Edit, review and duplicate choices. */
final class SetupSummary {
    private SetupSummary() { }
    static String format(TokenDescriptor descriptor) {
        return descriptor.algorithm().name() + " · " + descriptor.digits() + " digits · "
                + descriptor.period().getSeconds() + " seconds";
    }
}
