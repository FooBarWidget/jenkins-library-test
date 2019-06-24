package org

def foo() {
	def parts = env.GIT_BRANCH.split("/", 2)
	return parts[1]
}

return this
