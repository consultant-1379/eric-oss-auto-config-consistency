/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.TestDefaults.FDN_EXAMPLE_WITH_ENODEBFUNC;
import static com.ericsson.oss.apps.util.TestDefaults.FDN_EXAMPLE_WITH_NRCELL;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_CM_HANDLE;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_FDN;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.apps.service.exception.UnsupportedOperationException;

public class RanUtilitiesTest {

    @Test
    public void testConvertFdnToCmHandle() {

        final String cmHandle = RanUtilities.convertFdnToCmHandle(MANAGED_ELEMENT_1_FDN);
        assertThat(cmHandle).isEqualTo(MANAGED_ELEMENT_1_CM_HANDLE);
    }

    @Test
    public void testGetFdnDownToManagedElement() {

        final String downToFdn = RanUtilities.getFdnDownToManagedElement(FDN_EXAMPLE_WITH_NRCELL);

        assertThat(downToFdn).isEqualTo(MANAGED_ELEMENT_1_FDN);
    }

    @Test
    public void testFdnToResourceIdentifier() throws Exception {
        final String resourceIdentifier = RanUtilities.fdnToResourceIdentifier(FDN_EXAMPLE_WITH_ENODEBFUNC, "ENodeBFunction");
        assertThat(resourceIdentifier)
                .isEqualTo("/ericsson-enm-ComTop:ManagedElement[@id=NR03gNodeBRadio00030]");

        final String moFdnDU = MANAGED_ELEMENT_1_FDN + ",GNBDUFunction=1";
        final String resourceIdentifierDU = RanUtilities.fdnToResourceIdentifier(moFdnDU, "GNBDUFunction");
        assertThat(resourceIdentifierDU)
                .isEqualTo("/ericsson-enm-ComTop:ManagedElement[@id=NR03gNodeBRadio00030]");

        final String moFdnCUCP = MANAGED_ELEMENT_1_FDN + ",GNBCUCPFunction=1";
        final String resourceIdentifierCUCP = RanUtilities.fdnToResourceIdentifier(moFdnCUCP, "GNBCUCPFunction");
        assertThat(resourceIdentifierCUCP)
                .isEqualTo("/ericsson-enm-ComTop:ManagedElement[@id=NR03gNodeBRadio00030]");

        final String moFdnCUUP = MANAGED_ELEMENT_1_FDN + ",GNBCUUPFunction=1";
        final String resourceIdentifierCUUP = RanUtilities.fdnToResourceIdentifier(moFdnCUUP, "GNBCUUPFunction");
        assertThat(resourceIdentifierCUUP)
                .isEqualTo("/ericsson-enm-ComTop:ManagedElement[@id=NR03gNodeBRadio00030]");
    }

    @Test
    public void testFdnToResourceIdentifierForCells() throws Exception {
        final String resourceIdentifier = RanUtilities.fdnToResourceIdentifier(FDN_EXAMPLE_WITH_ENODEBFUNC + ",EUtranCellFDD=LTECellFDD10", "EUtranCellFDD");
        assertThat(resourceIdentifier)
                .isEqualTo("/ericsson-enm-ComTop:ManagedElement[@id=NR03gNodeBRadio00030]/ericsson-enm-Lrat:ENodeBFunction[@id=1]");

        final String moFdnDUCell = MANAGED_ELEMENT_1_FDN + ",GNBDUFunction=1,NRCellDU=NR09";
        final String resourceIdentifierDU = RanUtilities.fdnToResourceIdentifier(moFdnDUCell, "NRCellDU");
        assertThat(resourceIdentifierDU)
                .isEqualTo("/ericsson-enm-ComTop:ManagedElement[@id=NR03gNodeBRadio00030]/ericsson-enm-GNBDU:GNBDUFunction[@id=1]");

        final String moFdnCUCPCell = MANAGED_ELEMENT_1_FDN + ",GNBCUCPFunction=1,NRCellCU=NR30";
        final String resourceIdentifierCUCP = RanUtilities.fdnToResourceIdentifier(moFdnCUCPCell, "NRCellCU");
        assertThat(resourceIdentifierCUCP)
                .isEqualTo("/ericsson-enm-ComTop:ManagedElement[@id=NR03gNodeBRadio00030]/ericsson-enm-GNBCUCP:GNBCUCPFunction[@id=1]");
    }

    @Test
    public void testFdnToResourceIdentifierForMosBelowTheTechFunction() throws Exception {
        final String fdnLRat = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Tampa,MeContext=MA2H3009A2_50k_sim," +
                "ManagedElement=MA2H3009A2_50k_sim,ENodeBFunction=1,QciTable=default,QciProfileOperatorDefined=qci128";
        final String resourceIdentifierLRat = RanUtilities.fdnToResourceIdentifier(fdnLRat, "QciProfileOperatorDefined");

        assertThat(resourceIdentifierLRat)
                .isEqualTo(
                        "/ericsson-enm-ComTop:ManagedElement[@id=MA2H3009A2_50k_sim]/ericsson-enm-Lrat:ENodeBFunction[@id=1]/ericsson-enm-Lrat:QciTable[@id=default]");

        final String fdnGNBDU = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Tampa,MeContext=MA2H3009A2_50k_sim," +
                "ManagedElement=MA2H3009A2_50k_sim,GNBDUFunction=1,NRSector=nrs1,CommonBeamforming=1";
        final String resourceIdentifierGNBDU = RanUtilities.fdnToResourceIdentifier(fdnGNBDU, "CommonBeamforming");

        assertThat(resourceIdentifierGNBDU)
                .isEqualTo(
                        "/ericsson-enm-ComTop:ManagedElement[@id=MA2H3009A2_50k_sim]/ericsson-enm-GNBDU:GNBDUFunction[@id=1]/ericsson-enm-GNBDU:NRSector[@id=nrs1]");

        final String fdnGNBCUCP = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Tampa,MeContext=MA2H3009A2_50k_sim," +
                "ManagedElement=MA2H3009A2_50k_sim,GNBCUCPFunction=1,NRCellCU=nrcell1,UeMeasControl=1,ReportConfigA3=1";
        final String resourceIdentifierGNBCUCP = RanUtilities.fdnToResourceIdentifier(fdnGNBCUCP, "ReportConfigA3");

        assertThat(resourceIdentifierGNBCUCP)
                .isEqualTo(
                        "/ericsson-enm-ComTop:ManagedElement[@id=MA2H3009A2_50k_sim]/ericsson-enm-GNBCUCP:GNBCUCPFunction[@id=1]/ericsson-enm-GNBCUCP:NRCellCU[@id=nrcell1]/ericsson-enm-GNBCUCP:UeMeasControl[@id=1]");

        final String fdnGNBCUUP = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Tampa,MeContext=MA2H3009A2_50k_sim,ManagedElement=MA2H3009A2_50k_sim,GNBCUUPFunction=1,CUUP5qiTable=table10,CUUP5qi=cuup5q13";
        final String resourceIdentifierGNBCUUP = RanUtilities.fdnToResourceIdentifier(fdnGNBCUUP, "CUUP5qi");

        assertThat(resourceIdentifierGNBCUUP)
                .isEqualTo(
                        "/ericsson-enm-ComTop:ManagedElement[@id=MA2H3009A2_50k_sim]/ericsson-enm-GNBCUUP:GNBCUUPFunction[@id=1]/ericsson-enm-GNBCUUP:CUUP5qiTable[@id=table10]");

    }

    @Test
    public void testFdnToResourceIdentifierForManagedElement() throws Exception {
        final String fdnForManagedElement = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Tampa,MeContext=MA2H3009A2_50k_sim," +
                "ManagedElement=MA2H3009A2_50k_sim";
        final String resourceIdentifierManagedElement = RanUtilities.fdnToResourceIdentifier(fdnForManagedElement, "ManagedElement");
        assertThat(resourceIdentifierManagedElement).isEqualTo("/");
    }

    @Test
    public void testFdnToResourceIdentifierForNotSupportedTechFunction() {
        final String fdnNotSupported = "SubNetwork=ONRM_ROOT_MO,SubNetwork=Tampa,MeContext=MA2H3009A2_50k_sim," +
                "ManagedElement=MA2H3009A2_50k_sim,SystemFunctions=1,SwInventory=1,SwVersion=10";

        assertThatThrownBy(() -> RanUtilities.fdnToResourceIdentifier(fdnNotSupported, "SwVersion"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Could not get module for moFdn as the function is not a currently supported type " + fdnNotSupported);
    }
}
