# ==============================================================================
# Makefile for INFO403 - Part 3: YALCC Compiler (LLVM Code Gen)
# Authors: Zakaria M'Hamdi & Wassim Turk
# Date   : December 2025
# ==============================================================================

JAVAC   = javac
JAVA    = java
JFLEX   = jflex
JAVADOC = javadoc
LATEX   = pdflatex

SRC_DIR  = src
DIST_DIR = dist
TEST_DIR = test
DOC_DIR  = doc
MORE_DIR = more

LEX_FILE    = $(SRC_DIR)/LexicalAnalyzer.flex
MAIN_CLASS  = src.Main
JAR_FILE    = $(DIST_DIR)/part3.jar
JAVA_FILES  = $(wildcard $(SRC_DIR)/*.java)

REPORT_TEX  = INFO403_Part3_Report.tex
REPORT_PDF  = INFO403_Part3_Report.pdf

# ==============================================================================
# FULL BUILD
# ==============================================================================

all: $(JAR_FILE)

# Generate the scanner from JFlex specification
$(SRC_DIR)/LexicalAnalyzer.java: $(LEX_FILE)
	@echo "==> Generating LexicalAnalyzer.java with JFlex..."
	$(JFLEX) $(LEX_FILE)

# Compile Java sources and build the JAR
$(JAR_FILE): $(JAVA_FILES) $(SRC_DIR)/LexicalAnalyzer.java
	@echo "==> Building JAR..."
	@mkdir -p $(DIST_DIR)
	@rm -rf $(DIST_DIR)/*
	@echo "==> Compiling Java sources..."
	$(JAVAC) -d $(DIST_DIR) -sourcepath $(SRC_DIR) $(SRC_DIR)/*.java

	@echo "==> Creating JAR file..."
	@echo "Main-Class: $(MAIN_CLASS)" > MANIFEST.MF
	jar cfm $(JAR_FILE) MANIFEST.MF -C $(DIST_DIR) .
	@rm -f MANIFEST.MF
	@echo "==> JAR created: $(JAR_FILE)"

# ==============================================================================
# DOCUMENTATION
# ==============================================================================

doc: javadoc pdf
	@echo "==> Documentation (Javadoc + PDF) generated successfully."

javadoc: $(JAR_FILE)
	@echo "==> Generating Javadoc..."
	@mkdir -p $(DOC_DIR)/html
	$(JAVA) -jar $(JAR_FILE) >/dev/null 2>&1 || true
	$(JAVADOC) -d $(DOC_DIR)/html -sourcepath $(SRC_DIR) $(SRC_DIR)/*.java

pdf:
	@echo "==> Compiling LaTeX report (Part 3)..."
	@cd $(DOC_DIR) && $(LATEX) -interaction=nonstopmode $(REPORT_TEX) >/dev/null
	@cd $(DOC_DIR) && $(LATEX) -interaction=nonstopmode $(REPORT_TEX) >/dev/null
	@echo "==> PDF generated: $(DOC_DIR)/$(REPORT_PDF)"

# ==============================================================================
# EXECUTION & AUTOMATION (LLVM PIPELINE)
# ==============================================================================

run: $(JAR_FILE)
	@if [ -z "$(FILE)" ]; then \
		echo "Error: Usage: make run FILE=test/file.ycc"; \
		exit 1; \
	fi
	@mkdir -p $(MORE_DIR)
	@echo "==> [1/3] Compiling $(FILE) to LLVM IR..."
	@$(JAVA) -jar $(JAR_FILE) "$(FILE)" | tee $(MORE_DIR)/output.ll
	@echo ""
	@echo "==> LLVM IR saved to $(MORE_DIR)/output.ll"
	@echo ""
	@echo "==> [2/3] Assembling IR to Bitcode..."
	@llvm-as $(MORE_DIR)/output.ll -o $(MORE_DIR)/output.bc
	@echo "==> [3/3] Executing with LLVM JIT..."
	@echo "----------------------------------------"
	@lli $(MORE_DIR)/output.bc
	@echo "----------------------------------------"



runTest: $(JAR_FILE)
	@echo "==> Running Full Test Suite (LLVM Execution)..."
	@mkdir -p $(MORE_DIR)
	@for file in $(TEST_DIR)/*.ycc; do \
		echo "****************************************"; \
		echo "TEST: $$file"; \
		BASENAME=$$(basename $$file .ycc); \
		$(JAVA) -jar $(JAR_FILE) "$$file" | tee $(MORE_DIR)/$${BASENAME}.ll; \
		if llvm-as $(MORE_DIR)/$${BASENAME}.ll -o $(MORE_DIR)/$${BASENAME}.bc; then \
			echo "OUTPUT:"; \
			lli $(MORE_DIR)/$${BASENAME}.bc; \
		else \
			echo "FAIL: Assembler error."; \
		fi; \
		echo ""; \
	done
	@echo "==> All tests completed."



# ==============================================================================
# LEGACY TARGETS (PART 2) - DEPRECATED IN PART 3
# ==============================================================================
# NOTE: These will likely fail because Main.java no longer supports -wt args.

runWT:
	@echo "Error: -wt (Write Tree) is not supported in Part 3 Main.java"

runWTTest:
	@echo "Error: -wt (Write Tree) is not supported in Part 3 Main.java"

# ==============================================================================
# CLEANUP
# ==============================================================================

clean:
	@echo "==> Cleaning generated files..."
	@rm -f $(SRC_DIR)/*.class
	@rm -f $(SRC_DIR)/*~
	@rm -rf $(DIST_DIR)/src/*
	@rm -rf $(MORE_DIR)/*.tex
	@rm -rf $(MORE_DIR)/*.pdf
	@rm -rf $(MORE_DIR)/*.bc
	@rm -rf $(MORE_DIR)/*.ll
	@rm -f *.ll *.bc
	@find $(DOC_DIR) -maxdepth 1 -type f \( -name "*.aux" -o -name "*.log" -o -name "*.out" \) -delete
	@echo "==> Cleanup complete."

.PHONY: all doc javadoc pdf run runTest clean
