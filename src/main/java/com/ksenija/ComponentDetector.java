package com.ksenija;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Detects the type and mounting style of an electronic component
 * from its English description and CalcuQuote taxonomy string.
 * <p>
 * Used by both {@link ClassificationService} and {@link TranslatorService}
 * as a single shared source of keyword and pattern matching logic.
 * This means adding a new keyword only needs to be done in one place.
 * <p>
 * Detection combines both fields into one lowercase string and checks it
 * against keywords, taxonomy codes, and MPN prefix patterns.
 */
@Component
public class ComponentDetector {
    public ComponentType detect(String description, String taxonomy){
        String descPart = (description != null) ? description.toLowerCase().trim() : "";
        String taxPart  = (taxonomy != null)    ? taxonomy.toLowerCase().trim()    : "";
        String t = (descPart + " " + taxPart).trim();

        if (t.isEmpty()) return ComponentType.UNKNOWN;

        if (t.contains("vtičnica") || t.contains("vticnica"))
            return ComponentType.VTICNICA;
        if (t.contains("vijak"))
            return ComponentType.VIJAK;
        if (t.contains("embalaza") || t.contains("embalaža"))
            return ComponentType.EMBALAZA;
        if (t.contains("mechanical")) //hladilnik...

            return ComponentType.MECHANICAL;
        if (t.contains("lepilo")) //term. lepilo,..
            return ComponentType.LEPILO;


        if (t.contains("resistor") || t.contains("res /") || t.contains("upor") ||
                t.contains("resc") || t.contains("res-smd") ||          // taxonomy keywords
                matchesPattern(descPart, "\\b(crcw|erjm|erj|rc0|rc1|crgp|wsl|pwr)") || // MPN prefixes
                matchesPattern(t, "\\d+[,.]?\\d*\\s*(kohm|mohm|ohm|[mkrR]\\b)") ||
                matchesPattern(t, "[rR]\\d{4}\\b") ||
                (t.contains("thick film") && t.contains("ohm")))
            return ComponentType.RESISTOR;


        if (t.contains("capacitor") || t.contains("konde") ||
                matchesPattern(t, "\\d+[,.]?\\d*\\s*[uµnp]f\\b"))
            return ComponentType.CAPACITOR;


        if (t.startsWith("led") || t.contains("leds - smd") || t.contains("standard led") ||
                t.contains("dioda led") || t.contains("led dioda") ||
                matchesPattern(descPart, "\\b(tlms)\\d+"))  // Vishay LEDs
            return ComponentType.LED;


        if (t.contains("diode") || t.contains("dioda") || t.contains("zener") ||
                t.contains("tvs") || t.contains("schottky") || t.contains("esd") ||
                t.contains("bat4") || t.contains("bat8") ||
                matchesPattern(t, "sod-?\\d+") || matchesPattern(t, "\\bdo-?\\d+"))
            return ComponentType.DIODE;


        if (t.contains("mosfet") || t.contains("transistor") || t.contains("tranzistor") ||
                t.contains("igbt") || t.contains("tdson") || t.contains("tson") ||
                matchesPattern(t, "\\b(iauc|iaua|irf|ipp|bsc)\\d+"))
            return ComponentType.MOSFET;


        if (t.contains("integrirano vezje") || t.contains("integrated circuit") || matchesPattern(t, "\\bic\\b") ||
                t.contains("int. v.") || t.contains("buffer") || t.contains("driver") ||
                t.contains("regulator") || t.contains("controller") || t.contains("amplifier") ||
                t.contains("converter") || t.contains("microcontroller") || t.contains("memory") ||
                t.contains("comparator") || t.contains("transceiver") ||
                matchesPattern(descPart, "\\b(sn74|74hc|74ls|74ac|tle|xmc|stm32|pic|atmega|attiny|lm|lt|tl|mc|ad|max|ncp|mcp|drv|ina|opa)"))
            return ComponentType.IC;



        if (t.contains("connector") || t.contains("konektor") || t.contains("con-socket") ||
                t.contains("con-ter") ||
                t.contains("shorting pin") || t.contains("jumper"))
            return ComponentType.CONNECTOR;


        if (t.contains("header") || t.contains("letvica"))
            return ComponentType.HEADER;


        if (t.contains("inductor") || t.contains("ferrite") || t.contains("bead") ||
                t.contains("choke") || t.contains("tuljava") || t.contains("dušilka") ||
                t.contains("dusilka") || matchesPattern(t, "\\bind\\b"))
            return ComponentType.INDUCTOR;


        if (t.contains("crystal") || t.contains("quartz"))
            return ComponentType.CRYSTAL;


        if (t.contains("oscillator"))
            return ComponentType.OSCILLATOR;


        if (t.contains("fuse") || t.contains("varovalka"))
            return ComponentType.FUSE;


        if (t.contains("switch") || t.contains("stikalo"))
            return ComponentType.SWITCH;


        if (t.contains("pcb") || t.contains("tiv") || t.contains("printed circuit"))
            return ComponentType.PCB;

        return ComponentType.UNKNOWN;
    }


    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the given text contains a match for the regex pattern.
     *
     * @param text      text to search
     * @param pattern   regex pattern to match against
     * @return          {@code true} if a match is found
     */
    private boolean matchesPattern(String text, String pattern){
        return Pattern.compile(pattern).matcher(text).find();
//        String s = "(?i)\\b(sn74|74hc|74ls|74ac|tle|xmc|stm32|pic|atmega|attiny|lm|lt|tl|mc|ad|max|ncp|mcp|drv|ina|opa)[0-9][a-z0-9\\-]*";
//        Pattern pat = Pattern.compile(s);
//        boolean ima =  pat.matcher(text).find();
//        return ima;
    }

    /**
     * Returns {@code true} if the component is through-hole (THT) mounted.
     * It checks for keywords: "through hole", "tht" and "dip".
     *
     * @param description   English component description
     * @param taxonomy      component category string
     * @return              {@code true} if THT, {@code false} if SMD (default)
     */
    public boolean isTht(String description, String taxonomy) {
        String descPart = (description != null) ? description.toLowerCase().trim() : "";
        String taxPart  = (taxonomy != null)    ? taxonomy.toLowerCase().trim()    : "";
        String t = (descPart + " " + taxPart).trim();
        return t.contains("through hole") || t.contains("tht") || t.contains("dip");
    }

    /**
     * Detects the component package/case type from the description.
     * <p>
     * Checks for common SMD sizes (0402, 0805...) and IC packages (SOT, QFN, SOIC...).
     *
     * @param description English component description
     * @return detected package string in uppercase (e.g. "0805", "SOT-23"), or {@code ""} if not found
     */
    public String detectCase(String description) {
        if (description == null) return "";
        String d = description.toLowerCase().trim();
        String[] patterns = {
                "(0201|0402|0603|0805|1206|1210|1812|2010|2512)",
                "(sod-\\d+\\w*)", "(do-\\d+\\w*)", "(sot-\\d+\\w*)",
                "(to-\\d+\\w*)",  "(soic-?\\d*)",  "(qfp-?\\d*)",
                "(qfn-?\\d*)",    "(tsop-?\\d*)",  "(dpak|d2pak)", "(sma\\b)"
        };
        for (String pattern : patterns) {
            java.util.regex.Matcher m = Pattern.compile(pattern).matcher(d);
            if (m.find()) return m.group(1).toUpperCase();
        }
        return "";
    }

    /**
     * Detects the mounting type of component: THT, SMT, or SMD (default).
     *
     * @param description English component description
     * @param taxonomy    component category string
     * @return {@code "THT"}, {@code "SMT"}, or {@code "SMD"}
     */
    public String detectMount(String description, String taxonomy) {
        String descPart = (description != null) ? description.toLowerCase().trim() : "";
        String taxPart  = (taxonomy != null)    ? taxonomy.toLowerCase().trim()    : "";
        String t = (descPart + " " + taxPart).trim();
        if (t.contains("through hole") || t.contains("tht") || t.contains("dip")) return "THT";
        if (t.contains("smt") || t.contains("tsop"))                               return "SMT";
        return "SMD";
    }
}
