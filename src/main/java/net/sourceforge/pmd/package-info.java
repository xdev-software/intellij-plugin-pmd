/**
 * This packages contain various performance improvements for PMD.
 * <p>
 * Currently:
 * <ul>
 *     <li>
 *         <a href="https://github.com/pmd/pmd/issues/6155">pmd#6155</a>
 *         <ul>
 *             <li>RuleSets: Do not initialize Rules that have already been initialized</li>
 *             <li>RuleSets: Do not deep copy rules - which causes them to lose their initialization data</li>
 *             <li>SaxonXPathRuleQuery: Reuse cached configuration</li>
 *        </ul>
 *     </li>
 *     <li>
 *         Other
 *         <ul>
 *             <li>ZIPFileFingerprinter: Only fingerprint files when they were changed otherwise use cache</li>
 *         </ul>
 *     </li>
 * </ul>
 * </p>
 */
package net.sourceforge.pmd;
