package com.ksenija;

/**
 * Represents the detected type of electronic or mechanical component.
 * <p>
 * Used by {@link ComponentDetector} to classify a component from its description and taxonomy,
 * and consumed by both {@link ClassificationService} (to determine the Largo commodity group)
 * and {@link TranslatorService} (to generate the correct Slovenian name prefix).
 */
public enum ComponentType {
    /** Resistor, SMD or THT. */
    RESISTOR,

    /** Capacitor, ceramic, electrolytic, tantalum, etc. */
    CAPACITOR,

    /** LED, light-emitting diode. Checked before DIODE to avoid misclassification :). */
    LED,

    /** Diode, includes Zener, TVS, Schottky, ESD protection diodes. */
    DIODE,

    /** MOSFET or transistor, includes IGBT. */
    MOSFET,

    /** Integrated circuit, includes drivers, controllers, regulators, microcontrollers, etc. */
    IC,

    /** Connector, includes jumpers and shorting pins. */
    CONNECTOR,

    /** Header/pin strip (letvica). Always classified as THT. */
    HEADER,

    /** Inductor, ferrite bead, or choke. */
    INDUCTOR,

    /** Crystal resonator. */
    CRYSTAL,

    /** Oscillator. */
    OSCILLATOR,

    /** Fuse. */
    FUSE,

    /** Switch or push button. */
    SWITCH,

    /** Printed Circuit Board (PCB/TIV). */
    PCB,

    /** Socket/outlet . */
    VTICNICA,

    /** Packaging material (embalaža). */
    EMBALAZA,

    /** Screw or bolt. */
    VIJAK,

    /** Generic mechanical component. */
    MECHANICAL,

    /** Adhesive/thermal paste. */
    LEPILO,

    /** Component type could not be determined from description or taxonomy. */
    UNKNOWN
}
