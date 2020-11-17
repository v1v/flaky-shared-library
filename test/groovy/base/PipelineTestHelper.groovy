package base

import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString

class PipelineTestHelper extends DeclarativePipelineTest {

    @Override
    void setUp() {
        super.setUp()
        registerDeclarativeMethods()
        registerScriptedMethods()
        registerSharedLibraryMethods()
    }

    void registerDeclarativeMethods() {
    }

    void registerScriptedMethods() {
    }

    void registerSharedLibraryMethods() {
        helper.registerAllowedMethod('isBranch', {
            def script = loadScript('vars/isBranch.groovy')
            return script.call()
        })
    }

    def assertMethodCallContainsPattern(String methodName, String pattern) {
        return helper.callStack.findAll { call ->
            call.methodName == methodName
        }.any { call ->
            callArgsToString(call).contains(pattern)
        }
    }
}