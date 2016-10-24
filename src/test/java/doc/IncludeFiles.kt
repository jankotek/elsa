package doc

import org.junit.Test
import java.io.File

class IncludeFiles{

    @Test fun includeFiles(){
        recurDir(File("."))
    }

    fun recurDir(f:File){
        if(f.isDirectory)
            f.listFiles().forEach { recurDir(it) }
        if(f.isFile && f.name.endsWith(".md"))
            updateFile(f)
    }

    private fun updateFile(f: File) {
        try {
            val ss = f.readText().split("<!---")

            if (ss.size == 0)
                return

            println("Update examples in $f")

            var res = ss.first()

            for (s in ss.drop(1)) {
                val tag = s.split("--->").first()
                val marks = tag.replace(" ", "").split("#")
                if (marks.size < 2 || marks[1] != "file")
                    continue

                //get stuff
                val sourceFile = marks[2]
                val syntax = marks.getOrElse(3, { "java" })
                val sign = marks.getOrElse(4, { "" })

                //pass mark
                res += "<!---$tag--->\n"
                //skip ``` if they exists
                val tripleMarkPos = s.indexOf("```");
                val endPos =
                        if (tripleMarkPos != -1 && tripleMarkPos < tag.length + 20) {
                            s.indexOf("```", tripleMarkPos + 3) + 3
                        } else {
                            s.indexOf("--->") + 5
                        }
                val prefix = 8

                //read file content
                var source = File("src/test/java/" + sourceFile)
                        .readText()
                        .split("//#a" + sign)[1]
                        .split("//#z" + sign)[0]

                res += "```"+syntax
                for (line in source.lines().dropLast(1)) {
                    val index = if (line.length > prefix && line.substring(0, prefix).replace(" ", "").isEmpty()) prefix else 0
                    res += line.substring(index) + "\n"
                }

                res += "```"
                if (endPos != -1 && endPos < s.length)
                    res += s.substring(endPos, s.length)
            }

            f.writeText(res)
        } catch(e: Exception) {
            throw RuntimeException("error in file $f", e)
        }
    }
}