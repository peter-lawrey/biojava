
package org.biojava.bio.structure.io;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.PDBHeader;
import org.biojava.bio.structure.Structure;
import org.biojava.bio.structure.StructureException;
import org.biojava.bio.structure.StructureTools;
import org.biojava.bio.structure.align.util.AtomCache;
import org.biojava.bio.structure.quaternary.BioAssemblyInfo;
import org.biojava.bio.structure.quaternary.BiologicalAssemblyTransformation;
import org.biojava.bio.structure.quaternary.io.BioUnitDataProviderFactory;
import org.biojava.bio.structure.quaternary.io.MmCifBiolAssemblyProvider;
import org.biojava.bio.structure.quaternary.io.PDBBioUnitDataProvider;
import org.biojava3.structure.StructureIO;
import org.junit.Test;

public class TestQuaternaryStructureProviders {

	@Test
	public void test1STP() throws IOException, StructureException{
		testID("1stp",1);
	}
	
	@Test
	public void test3FAD() throws IOException, StructureException{
		testID("3fad",1);
		testID("3fad",2);
	}
	
	@Test
	public void test5LDH() throws IOException, StructureException{
		testID("5LDH",1);
	}
	
	@Test
	public void test3NTU() throws IOException, StructureException{
		testID("3NTU",1);
	}
	
	@Test
	public void test1A29() throws IOException, StructureException{
		testID("1A29",1);
	}
	
//	@Test
//	public void test1EI7(){
//		testID("1ei7",1);
//	}
	
		
	private void testID(String pdbId, int bioMolecule) throws IOException, StructureException{
		
		// get bio assembly from PDB file
		PDBBioUnitDataProvider pdbProvider = new PDBBioUnitDataProvider();
		BioUnitDataProviderFactory.setBioUnitDataProvider(pdbProvider.getClass().getCanonicalName());
		Structure pdbS = StructureIO.getBiologicalAssembly(pdbId, bioMolecule);

		// get bio assembly from mmcif file
		MmCifBiolAssemblyProvider mmcifProvider = new MmCifBiolAssemblyProvider();
		BioUnitDataProviderFactory.setBioUnitDataProvider(mmcifProvider.getClass().getCanonicalName());			
		Structure mmcifS = StructureIO.getBiologicalAssembly(pdbId, bioMolecule);

		BioUnitDataProviderFactory.setBioUnitDataProvider(BioUnitDataProviderFactory.DEFAULT_PROVIDER_CLASSNAME);



		PDBHeader pHeader = pdbS.getPDBHeader();
		PDBHeader mHeader = mmcifS.getPDBHeader();
		//PDBHeader fHeader = flatFileS.getPDBHeader();

		assertTrue("not correct nr of bioassemblies " + pHeader.getNrBioAssemblies() + " " , pHeader.getNrBioAssemblies() >= bioMolecule);
		assertTrue("not correct nr of bioassemblies " + mHeader.getNrBioAssemblies() + " " , mHeader.getNrBioAssemblies() >= bioMolecule);
		//assertTrue("not correct nr of bioassemblies " + fHeader.getNrBioAssemblies() + " " , fHeader.getNrBioAssemblies() >= bioMolecule);

		// mmcif files contain sometimes partial virus assemblies, so they can contain more info than pdb
		assertTrue(pHeader.getNrBioAssemblies() <= mHeader.getNrBioAssemblies());


		Map<Integer, BioAssemblyInfo> pMap = pHeader.getBioAssemblies();
		Map<Integer, BioAssemblyInfo> mMap = mHeader.getBioAssemblies();

		//System.out.println("PDB: " + pMap);

		//System.out.println("Mmcif: " + mMap);

		assertTrue(pMap.keySet().size()<= mMap.keySet().size());

		for ( int k : pMap.keySet()) {
			assertTrue(mMap.containsKey(k));

			assertEquals("Macromolecular sizes don't coincide!",pMap.get(k).getMacromolecularSize(), mMap.get(k).getMacromolecularSize());
			
			List<BiologicalAssemblyTransformation> pL = pMap.get(k).getTransforms();

			// mmcif list can be longer due to the use of internal chain IDs
			List<BiologicalAssemblyTransformation> mL = mMap.get(k).getTransforms();

			//assertEquals(pL.size(), mL.size());


			for (BiologicalAssemblyTransformation m1 : pL){

				boolean found = false;
				for ( BiologicalAssemblyTransformation m2 : mL){

					if  (! m1.getChainId().equals(m2.getChainId()))
						continue;
					
					if ( ! m1.getTransformationMatrix().epsilonEquals(m2.getTransformationMatrix(), 0.0001)) 
						continue;

					found = true;

				}

				if ( ! found ){
					System.err.println("did not find matching matrix " + m1);
					System.err.println(mL);
				}
				assertTrue(found);

			}
		}


		assertEquals("Not the same number of chains!" , pdbS.size(),mmcifS.size());

		Atom[] pdbA = StructureTools.getAllAtomArray(pdbS);

		Atom[] mmcifA = StructureTools.getAllAtomArray(mmcifS);

		assertEquals(pdbA.length, mmcifA.length);

		assertEquals(pdbA[0].toPDB(), mmcifA[0].toPDB());


		// compare with flat file version:
		AtomCache cache = new AtomCache();
		FileParsingParameters params = cache.getFileParsingParams();
		params.setAlignSeqRes(true);
		params.setParseCAOnly(false);

		Structure flatFileS = cache.getBiologicalAssembly(pdbId, bioMolecule, false);

		Atom[] fileA = StructureTools.getAllAtomArray(flatFileS);

		assertEquals(pdbA.length, fileA.length);

		assertEquals(pdbS.nrModels(),flatFileS.nrModels());

	}
	
}
