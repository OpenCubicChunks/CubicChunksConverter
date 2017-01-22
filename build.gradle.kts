import nl.javadude.gradle.plugins.license.LicenseExtension
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.exception.GrgitException
import org.ajoberstar.grgit.operation.DescribeOp
import org.eclipse.jgit.errors.RepositoryNotFoundException
import org.gradle.api.JavaVersion
import org.gradle.api.internal.HasConvention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.script.lang.kotlin.*

buildscript {
    repositories {
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("org.ajoberstar:grgit:1.4.+")
        classpath("gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.13.1")
    }
}

group = "cubicchunks"
version = getProjectVersion()

val licenseYear = properties["licenseYear"] as String
val projectName = properties["projectName"] as String

apply {
    plugin<JavaPlugin>()
    plugin<LicensePlugin>()
}

configure<JavaPluginConvention> {
    setSourceCompatibility(JavaVersion.VERSION_1_8)
}


configure<LicenseExtension> {
    val ext = (this as HasConvention).convention.extraProperties
    ext["project"] = projectName
    ext["year"] = licenseYear
    exclude("**/*.info")
    exclude("**/package-info.java")
    exclude("**/*.json")
    exclude("**/*.xml")
    exclude("assets/*")
    header = file("HEADER.txt")
    ignoreFailures = false
    strictCheck = true
    mapping(mapOf("java" to "SLASHSTAR_STYLE"))
}

repositories {
    mavenCentral()
    maven {
        setUrl("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    compile("com.flowpowered:flow-nbt:1.0.1-SNAPSHOT")
    compile(project("RegionLib"))
    testCompile("junit:junit:4.11")
}

//returns version string according to this: http://semver.org/
//format: MAJOR.MINOR.PATCH
fun getProjectVersion(): String {
    try {
        val git = Grgit.open()
        val describe = DescribeOp(git.repository).call()
        val branch = git.branch.current.name
        return getVersion_do(describe, branch);
    } catch(ex: RepositoryNotFoundException) {
        logger.error("Git repository not found! Version will be incorrect!")
        return getVersion_do("v9999-9999-gffffff", "localbuild")
    } catch(ex: GrgitException) {
        logger.error("Error getting version!")
        ex.printStackTrace()
        return getVersion_do("v9999-9999-gffffff", "localbuild")
    }
}

fun getVersion_do(describe: String, branch: String): String {

    val versionMinorFreeze = project.property("versionMinorFreeze") as String

    //branch "master" is not appended to version string, everything else is
    //only builds from "master" branch will actually use the correct versioning
    //but it allows to distinguish between builds from different branches even if version number is the same
    val branchSuffix = if (branch == "master") "" else ("-" + branch.replace("[^a-zA-Z0-9.-]", "_"))

    val baseVersionRegex = "v[0-9]+"
    val unknownVersion = String.format("UNKNOWN_VERSION%s", branchSuffix)
    if (!describe.contains('-')) {
        //is it the "vX" format?
        if (describe.matches(Regex(baseVersionRegex))) {
            return String.format("%s.0.0%s", describe, branchSuffix)
        }
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    //Describe format: vX-build-hash
    val parts = describe.split("-")
    if (!parts[0].matches(Regex(baseVersionRegex))) {
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    if (!parts[1].matches(Regex("[0-9]+"))) {
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    val apiVersion = parts[0].substring(1)
    //next we have commit-since-tag
    val commitSinceTag = Integer.parseInt(parts[1])

    val minorFreeze = if (versionMinorFreeze.isEmpty()) -1 else Integer.parseInt(versionMinorFreeze)

    val minor = if (minorFreeze < 0) commitSinceTag else minorFreeze
    val patch = if (minorFreeze < 0) 0 else (commitSinceTag - minorFreeze)

    val version = String.format("%s.%d.%d%s", apiVersion, minor, patch, branchSuffix)
    return version
}