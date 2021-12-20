package org.openjdk.jmc.flightrecorder.parser.deobfuscator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openjdk.jmc.common.collection.FastAccessNumberMap;
import org.openjdk.jmc.flightrecorder.parser.IConstantPoolExtension;
import org.openjdk.jmc.flightrecorder.parser.IParserExtension;

/**
 * inspired by ConstantPoolExtensionTest
 * 
 *
 */
public class DeobfuscatorParserExtension implements IParserExtension {
	@Override
	public IConstantPoolExtension createConstantPoolExtension() {
		return new DeobfuscatorConstantPoolExtension();
	}

	
	private static class DeobfuscatorConstantPoolExtension implements IConstantPoolExtension {
//		Set<String> readEventTypes = new HashSet<>();
//		Set<String> referencedEventTypes = new HashSet<>();
		private Deobfuscator deobfuscator;

		DeobfuscatorConstantPoolExtension() {
			
			// empty Deobfuscator for testing
//	    	Map<String, String> classMap = new HashMap<String, String>();
//	    	Map<String, String> packageMap = new HashMap<String, String>();
//	    	Map<String, Map<String, String>> classMethodsMap = new HashMap<String, Map<String, String>>();
//			deobfuscator = new Deobfuscator(classMap, packageMap, classMethodsMap);						

			// will become real Deobfuscator
//			deobfuscator = parseProguardMapping();

			// minimum implementation for demonstration 
			deobfuscator = createFooDeobfuscator(); 
		}
		
		@Override
		public Object constantRead(long constantIndex, Object constant, String eventTypeId) {
			if (constant instanceof String) {
                String sConstant = (String)constant;				
				
                if (sConstant.startsWith("[L")) {
                	String query = sConstant.substring(2, sConstant.length()-1); // between "[L" and ";"
    				String clazz = deobfuscator.getClass((String)query, false);
    				if (clazz != null) {
    					sConstant = sConstant.replaceFirst(query, clazz);
    					System.out.println(constant + " -> Array: " + sConstant);
    					return sConstant;
    				}
                }
                
				String clazz = deobfuscator.getClass((String)constant, false);
				if (clazz != null) {
					System.out.println(constant + " -> class: " + clazz);
					return clazz;
				}

				String pakkage = deobfuscator.getPackage((String)constant);
				if (pakkage != null) {
					System.out.println(constant + " -> package: " + pakkage);
					return pakkage;
				}
				System.out.println("else String: " + constant);
			}
			return constant;			
		}

		@Override
		public Object constantReferenced(Object constant, String poolName, String eventTypeId) {
//			referencedEventTypes.add(eventTypeId);
			return constant;
		}

//		Set<String> resolvedEventTypes = new HashSet<>();
//		private FastAccessNumberMap<Object> constantPool;

		@Override
		public Object constantResolved(Object constant, String poolName, String eventTypeId) {
//			resolvedEventTypes.add(eventTypeId);
			return constant;
		}

		@Override
		public void allConstantPoolsResolved(Map<String, FastAccessNumberMap<Object>> constantPools) {
//			for (int i = 0; i < POOL_NAMES.length; i++) {
//				constantPool = constantPools.get(POOL_NAMES[i]);
//				Assert.assertNotNull(constantPool);
//				int count = 0;
//				Iterator<Object> it = constantPool.iterator();
//				while (it.hasNext()) {
//					it.next();
//					count++;
//				}
//				Assert.assertEquals(POOL_SIZES[i], count);
//			}
		}

	}

	/**
	 * Test implementation to demonstrate error with current implementation
	 * - run JMC and open recording core\tests\org.openjdk.jmc.flightrecorder.serializers.test\target\classes\recordings\hotmethods.jfr
	 * - one class of the recording will be replaced by some other which simulates deobfuscation.
	 * - got to page Environment / Recording / Constant Pools
	 *   You get error "Constant Pools" could not be displayed and a stacktrace. 
	 * 
	 * @return
	 */
    private static Deobfuscator createFooDeobfuscator() {
    	Map<String, String> obfuscatorClassMap = new HashMap<String, String>();  // obfuscated class -> class
    	obfuscatorClassMap.put("se/hirt/jmc/tutorial/hotmethods/HotMethods", "de/docware/ms/foo/Foo");
		Map<String, String> obfuscatorPackageMap = new HashMap<String, String>();  // obfuscated package -> package
		Map<String, Map<String, String>> obfuscatorClassMethodsMap = new HashMap<String, Map<String, String>>();  // (class, obfuscated method) -> method
    	return new Deobfuscator(obfuscatorClassMap, obfuscatorPackageMap, obfuscatorClassMethodsMap);
    }
	
	/**
	 * Parse ProGuard obfuscation mapping file and obtain Deobfuscator helper class
	 * Path to Mapping file is found in properties file in program dir (or in the Eclipse program directory when running from Eclipse IDE).
	 * 
	 * @return
	 * @throws IOException
	 */
    private static Deobfuscator parseProguardMapping() {
    	Map<String, String> obfuscatorClassMap = new HashMap<String, String>();  // obfuscated class -> class
    	Map<String, String> obfuscatorPackageMap = new HashMap<String, String>();  // obfuscated package -> package
    	Map<String, Map<String, String>> obfuscatorClassMethodsMap = new HashMap<String, Map<String, String>>();  // (class, obfuscated method) -> method

    	try {
			// get path to mapping file from Properties file in working dir
			String proguardMappingFilePath = null;
			Properties props = new Properties();
			try (FileInputStream input = new FileInputStream(new File("proguard.properties"));
					InputStreamReader reader = new InputStreamReader(input, Charset.forName("UTF-8"))) {
				props.load(reader);
				proguardMappingFilePath = props.getProperty("file");
			}
			if (proguardMappingFilePath != null) {
				try (BufferedReader br = new BufferedReader(new FileReader(proguardMappingFilePath))) {
					String line;
					Map<String, String> methodsOfClassMap = null;
					while ((line = br.readLine()) != null) {
						String[] lineParts = line.split(" -> ");
						if (line.startsWith(" ")) {
							// field (TODO) or 
							// method or 
							// inner class (TODO)
							// TODO method lines start with <fromLine>:<toLine>, e.g. "    178:221:void setCustomSettings() -> b"; must be removed
//							String[] methodParts = lineParts[0].split(" ");
//							String methodName = methodParts[1];
//							String methodReturn = methodParts[0];  // TODO crop line numbers  
							methodsOfClassMap.put(lineParts[1], lineParts[0].trim());
						} else {
							// class (ConstantPool uses '/' rather than '.' as separator char)
							String clearClass = lineParts[0].replace('.', '/');
							String obfuscatedClass = lineParts[1].substring(0, lineParts[1].length() - 1).replace('.', '/'); // remove trailing colon  
							obfuscatorClassMap.put(obfuscatedClass, clearClass);
							
							// package
							String obfuscatedPackage = obfuscatedClass.substring(0, obfuscatedClass.lastIndexOf('/'));
							String clearPackage = clearClass.substring(0, clearClass.lastIndexOf('/'));
							obfuscatorPackageMap.put(obfuscatedPackage, clearPackage);
							

							methodsOfClassMap = new HashMap<>();
							obfuscatorClassMethodsMap.put(clearClass, methodsOfClassMap);
						}
					}
				}
			} 
    	} catch (Exception e) {
    		System.err.println("Error parsing ProGuard mapping file");
    		
    		// empty maps will just do nothing
        	obfuscatorClassMap = new HashMap<String, String>();
        	obfuscatorClassMethodsMap = new HashMap<String, Map<String, String>>();
		}

    	return new Deobfuscator(obfuscatorClassMap, obfuscatorPackageMap, obfuscatorClassMethodsMap);
    }	
	
	/**
     * Helper class for deobfuscation of class, package and method names
     * So far tested only for ProGuard, but should potentially work with any obfuscator.
     * 
     */
    private static class Deobfuscator {
    	private Map<String, String> classMap = new HashMap<String, String>();
    	private Map<String, String> packageMap = new HashMap<String, String>();
    	private Map<String, Map<String, String>> classMethodsMap = new HashMap<String, Map<String, String>>();

    	public Deobfuscator(Map<String, String> classMap, Map<String, String> packageMap, Map<String, Map<String, String>> classMethodsMap) {
    		this.classMap = classMap;
    		this.packageMap = packageMap;
    		this.classMethodsMap = classMethodsMap;
    	}
    	
    	/**
    	 * Deobfuscate class name 
    	 * 
    	 * @param clazz 
    	 *        qualified potentially obfuscated class name
    	 * @param qualified
    	 *        {@code true} if the returned string should be fully qualified 
    	 * @return deobfuscated class name or original class name if no deobfuscation is in effect or obfuscation mapping is wrong
    	 */
    	public String getClass(String clazz, boolean qualified) {
    		return classMap.get(clazz);
    		
//			String deobfuscatedClass = classMap.get(clazz);  // qualified
//    		clazz = (deobfuscatedClass != null) ? deobfuscatedClass : clazz;
//    		return clazz;
    	}

    	/**
    	 * Deobfuscate package name 
    	 * TODO no usage in JMC code yet
    	 * 
    	 * @param packageName
    	 *        potentially obfuscated package name
    	 * @return
    	 */
    	public String getPackage(String packageName) {
    		return packageMap.get(packageName);
    		
//			String deobfuscatedPackage = packageMap.get(packageName);
//			return (deobfuscatedPackage != null) ? deobfuscatedPackage : packageName;
    	}

    	/**
    	 * Deobfuscate method name
    	 * 
    	 * @param clazz
    	 *        deobfuscated, qualified class name
    	 * @param method
    	 *        obfuscated method name
    	 * @return deobfuscated method name or original method name if no deobfuscation is in effect or obfuscation mapping is wrong
    	 */
    	public String getMethod(String clazz, String method) {
;    		Map<String, String> methodsMap = classMethodsMap.get(clazz);
    		return methodsMap != null ? methodsMap.get(method) : method; 
    	}
    	
    }
}

