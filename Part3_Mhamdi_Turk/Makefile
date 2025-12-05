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

# Run pipeline on a single file: Java -> .ll -> llvm-as -> .bc -> lli -> Output
run: $(JAR_FILE)
	@if [ -z "$(FILE)" ]; then \
		echo "Error: Usage: make run FILE=test/file.ycc"; \
		exit 1; \
	fi
	@echo "==> [1/3] Compiling $(FILE) to LLVM IR..."
	@$(JAVA) -jar $(JAR_FILE) "$(FILE)" > output.ll
	@echo "==> [2/3] Assembling IR to Bitcode..."
	@llvm-as output.ll -o output.bc
	@echo "==> [3/3] Executing with LLVM JIT..."
	@echo "----------------------------------------"
	@lli output.bc
	@echo "\n----------------------------------------"
	@echo "==> Cleanup intermediate files."
	@rm -f output.ll output.bc

# Batch testing on all files in test/
runTest: $(JAR_FILE)
	@echo "==> Running Full Test Suite (LLVM Execution)..."
	@for file in $(TEST_DIR)/*.ycc; do \
		echo "****************************************"; \
		echo "TEST: $$file"; \
		$(JAVA) -jar $(JAR_FILE) "$$file" > test_tmp.ll; \
		if llvm-as test_tmp.ll -o test_tmp.bc; then \
			echo "OUTPUT:"; \
			lli test_tmp.bc; \
		else \
			echo "FAIL: Assembler error."; \
		fi; \
		echo ""; \
	done
	@rm -f test_tmp.ll test_tmp.bc
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
	@rm -f *.ll *.bc
	@find $(DOC_DIR) -maxdepth 1 -type f \( -name "*.aux" -o -name "*.log" -o -name "*.out" \) -delete
	@echo "==> Cleanup complete."

.PHONY: all doc javadoc pdf run runTest clean
# # ==============================================================================
# # Makefile for INFO403 - Part 3: YALCC Compiler (LLVM Code Gen)
# # Authors: Zakaria M'Hamdi & Wassim Turk
# # Date   : December 2025
# # ==============================================================================
# # BEFORE USE :
# #       -Download pdfLatex on your laptop ( sudo apt install texlive-full) to compile latex files to PDF
# #
# # USAGE:
# #   make                : Generate the scanner, compile sources and create the JAR
# #   make run FILE=...   : Run compiler on a specific file (Outputs LLVM IR)
# #                         Example: make run FILE=test/Euclid.ycc > output.ll
# #   make runTest        : Run compiler on all .ycc files in the test/ folder
# #   make doc            : Generate Javadoc and compile the LaTeX report into PDF
# #   make clean          : Remove temporary and generated files (but keep report PDF)
# #
# #   (Note: runWT targets are preserved but may not work with Part 3 Main.java)
# # ==============================================================================

# JAVAC   = javac
# JAVA    = java
# JFLEX   = jflex
# JAVADOC = javadoc
# LATEX   = pdflatex

# SRC_DIR  = src
# DIST_DIR = dist
# TEST_DIR = test
# DOC_DIR  = doc
# MORE_DIR = more

# LEX_FILE    = $(SRC_DIR)/LexicalAnalyzer.flex
# MAIN_CLASS  = src.Main
# JAR_FILE    = $(DIST_DIR)/part3.jar
# JAVA_FILES  = $(wildcard $(SRC_DIR)/*.java)

# REPORT_TEX  = INFO403_Part3_Report.tex
# REPORT_PDF  = INFO403_Part3_Report.pdf

# # ==============================================================================
# # FULL BUILD
# # ==============================================================================

# all: $(JAR_FILE)

# # Generate the scanner from JFlex specification
# $(SRC_DIR)/LexicalAnalyzer.java: $(LEX_FILE)
# 	@echo "==> Generating LexicalAnalyzer.java with JFlex..."
# 	$(JFLEX) $(LEX_FILE)

# # Compile Java sources and build the JAR
# $(JAR_FILE): $(JAVA_FILES) $(SRC_DIR)/LexicalAnalyzer.java
# 	@echo "==> Building JAR..."
# 	@mkdir -p $(DIST_DIR)
# 	# Remove any old compiled classes to avoid duplicate class errors
# 	@rm -rf $(DIST_DIR)/*
# 	@echo "==> Compiling Java sources..."
# 	$(JAVAC) -d $(DIST_DIR) -sourcepath $(SRC_DIR) $(SRC_DIR)/*.java

# 	@echo "==> Creating JAR file..."
# 	@echo "Main-Class: $(MAIN_CLASS)" > MANIFEST.MF
# 	jar cfm $(JAR_FILE) MANIFEST.MF -C $(DIST_DIR) .
# 	@rm -f MANIFEST.MF
# 	@echo "==> JAR created: $(JAR_FILE)"

# # ==============================================================================
# # DOCUMENTATION
# # ==============================================================================

# doc: javadoc pdf
# 	@echo "==> Documentation (Javadoc + PDF) generated successfully."

# # Generate Javadoc
# javadoc: $(JAR_FILE)
# 	@echo "==> Generating Javadoc..."
# 	@mkdir -p $(DOC_DIR)/html
# 	# Optionally run the JAR once (ignore errors)
# 	$(JAVA) -jar $(JAR_FILE) >/dev/null 2>&1 || true
# 	$(JAVADOC) -d $(DOC_DIR)/html -sourcepath $(SRC_DIR) $(SRC_DIR)/*.java
# 	@echo "==> Javadoc generated in $(DOC_DIR)/html/"

# # Compile LaTeX report into PDF
# pdf:
# 	@echo "==> Compiling LaTeX report (Part 3)..."
# 	@cd $(DOC_DIR) && $(LATEX) -interaction=nonstopmode $(REPORT_TEX) >/dev/null
# 	@cd $(DOC_DIR) && $(LATEX) -interaction=nonstopmode $(REPORT_TEX) >/dev/null
# 	@echo "==> PDF generated: $(DOC_DIR)/$(REPORT_PDF)"

# # ==============================================================================
# # EXECUTION
# # ==============================================================================

# # Run parser on a specific .ycc source file
# run: $(JAR_FILE)
# 	@if [ -z "$(FILE)" ]; then \
# 		echo "Error: you must specify a file."; \
# 		echo "Usage: make run FILE=test/yourfile.ycc"; \
# 		exit 1; \
# 	else \
# 		echo "==> Running JAR on $(FILE)"; \
# 		$(JAVA) -jar $(JAR_FILE) "$(FILE)"; \
# 	fi

# # Run parser on all test files inside the test directory
# runTest: $(JAR_FILE)
# 	@echo "==> Running all tests using JAR..."
# 	@for file in $(TEST_DIR)/*.ycc; do \
# 		echo ""; \
# 		echo "---------- $$file ----------"; \
# 		$(JAVA) -jar $(JAR_FILE) "$$file"; \
# 	done
# 	@echo ""
# 	@echo "==> All tests completed."

# # Run parser with -wt option (LEGACY/BROKEN in Part 3)
# runWT: $(JAR_FILE)
# 	@if [ -z "$(FILE)" ]; then \
# 	  echo "Error: you must specify a file."; \
# 	  echo "Usage: make runWT FILE=test/Euclid.ycc"; \
# 	  exit 1; \
# 	else \
# 	  mkdir -p $(MORE_DIR); \
# 	  BASENAME=$$(basename "$(FILE)"); \
# 	  OUTPUT="$(MORE_DIR)/$${BASENAME%.*}.tex"; \
# 	  echo "==> Running JAR on $(FILE) with -wt, output: $$OUTPUT"; \
# 	  $(JAVA) -jar $(JAR_FILE) -wt "$$OUTPUT" "$(FILE)"; \
# 	  echo "==> Compiling $$OUTPUT to PDF..."; \
# 	  $(LATEX) -interaction=nonstopmode -output-directory=$(MORE_DIR) "$$OUTPUT" >/dev/null; \
# 	  rm -f "$(MORE_DIR)/$${BASENAME%.*}.aux" "$(MORE_DIR)/$${BASENAME%.*}.log" "$(MORE_DIR)/$${BASENAME%.*}.out"; \
# 	fi

# # Run parser with -wt on all test/*.ycc files (LEGACY/BROKEN in Part 3)
# runWTTest: $(JAR_FILE)
# 	@echo "==> Running -wt on all test files..."
# 	@mkdir -p $(MORE_DIR)
# 	@for file in $(TEST_DIR)/*.ycc; do \
# 		BASENAME=$$(basename "$$file"); \
# 		OUTPUT="$(MORE_DIR)/$${BASENAME%.*}.tex"; \
# 		echo ""; \
# 		echo "---------- $$file ----------"; \
# 		echo "Output: $$OUTPUT"; \
# 		$(JAVA) -jar $(JAR_FILE) -wt "$$OUTPUT" "$$file"; \
# 		echo "==> Compiling $$OUTPUT to PDF..."; \
# 		$(LATEX) -interaction=nonstopmode -output-directory=$(MORE_DIR) "$$OUTPUT" >/dev/null; \
# 		rm -f "$(MORE_DIR)/$${BASENAME%.*}.aux" \
# 			  "$(MORE_DIR)/$${BASENAME%.*}.log" \
# 			  "$(MORE_DIR)/$${BASENAME%.*}.out"; \
# 	done
# 	@echo ""
# 	@echo "==> All -wt tests completed."

# # ==============================================================================
# # CLEANUP
# # ==============================================================================

# clean:
# 	@echo "==> Cleaning generated files..."
# # 	@rm -f $(SRC_DIR)/LexicalAnalyzer.java
# 	@rm -f $(SRC_DIR)/*.class
# 	@rm -f $(SRC_DIR)/*~
# 	@rm -rf $(DIST_DIR)/src/*
# 	@rm -rf $(MORE_DIR)/*.tex
# 	@rm -rf $(MORE_DIR)/*.pdf
# 	# Temporary LaTeX files for the report
# 	@find $(DOC_DIR) -maxdepth 1 -type f \( -name "*.aux" -o -name "*.log" -o -name "*.out" \) -delete
# 	# Temporary LaTeX files for the parse trees in more/
# 	@rm -f $(MORE_DIR)/*.aux $(MORE_DIR)/*.log $(MORE_DIR)/*.out
# 	# Auto-generated javac argument files
# 	@rm -f javac.*.args *.args
# 	@echo "==> Cleanup complete."

# .PHONY: all doc javadoc pdf run runTest runWT runWTTest clean

# # Deletes the jar
# cleanJar:
# 	@echo "==> Removing JAR files from dist..."
# 	@find $(DIST_DIR) -type f -name "*.jar" -delete
# 	@echo "==> JAR cleanup complete."
# # # ==============================================================================
# # # Makefile for INFO403 - Part 2: yaLcc Parser (LL(1) Recursive Descent)
# # # Authors: Zakaria M'Hamdi & Wassim Turk
# # # Date   : November 2025
# # # ==============================================================================
# # # BEFORE USE :
# # #		-Download pdfLatex on your laptop ( sudo apt install texlive-full) to compile latex files to PDF
# # #
# # # USAGE:
# # #   make                : Generate the scanner, compile sources and create the JAR
# # #   make run FILE=...   : Run parser on a specific file
# # #                         Example: make run FILE=test/Euclid.ycc
# # #   make runTest        : Run parser on all .ycc files in the test/ folder
# # #   make runWT FILE=... : Run parser with -wt to generate the LaTeX parse tree
# # #                         Example: make runWT FILE=test/Euclid.ycc
# # #						  PDF and latex are automatically generated in /more
# # #   make runWTTest      : Run parser with -wt on all test/*.ycc and build PDFs
# # # 						  PDF and latex are automatically generated in /more
# # #   make doc            : Generate Javadoc and compile the LaTeX report into PDF
# # #   make clean          : Remove temporary and generated files (but keep report PDF)
# # # ==============================================================================

# # JAVAC   = javac
# # JAVA    = java
# # JFLEX   = jflex
# # JAVADOC = javadoc
# # LATEX   = pdflatex

# # SRC_DIR  = src
# # DIST_DIR = dist
# # TEST_DIR = test
# # DOC_DIR  = doc
# # MORE_DIR = more

# # LEX_FILE    = $(SRC_DIR)/LexicalAnalyzer.flex
# # MAIN_CLASS  = src.Main
# # JAR_FILE    = $(DIST_DIR)/part2.jar
# # JAVA_FILES  = $(wildcard $(SRC_DIR)/*.java)

# # REPORT_TEX  = INFO403_Part2_Report.tex
# # REPORT_PDF  = INFO403_Part2_Report.pdf

# # # ==============================================================================
# # # FULL BUILD
# # # ==============================================================================

# # all: $(JAR_FILE)

# # # Generate the scanner from JFlex specification
# # $(SRC_DIR)/LexicalAnalyzer.java: $(LEX_FILE)
# # 	@echo "==> Generating LexicalAnalyzer.java with JFlex..."
# # 	$(JFLEX) $(LEX_FILE)

# # # Compile Java sources and build the JAR
# # $(JAR_FILE): $(JAVA_FILES) $(SRC_DIR)/LexicalAnalyzer.java
# # 	@echo "==> Building JAR..."
# # 	@mkdir -p $(DIST_DIR)
# # 	# Remove any old compiled classes to avoid duplicate class errors
# # 	@rm -rf $(DIST_DIR)/*
# # 	@echo "==> Compiling Java sources..."
# # 	$(JAVAC) -d $(DIST_DIR) -sourcepath $(SRC_DIR) $(SRC_DIR)/*.java

# # 	@echo "==> Creating JAR file..."
# # 	@echo "Main-Class: $(MAIN_CLASS)" > MANIFEST.MF
# # 	jar cfm $(JAR_FILE) MANIFEST.MF -C $(DIST_DIR) .
# # 	@rm -f MANIFEST.MF
# # 	@echo "==> JAR created: $(JAR_FILE)"

# # # ==============================================================================
# # # DOCUMENTATION
# # # ==============================================================================

# # doc: javadoc pdf
# # 	@echo "==> Documentation (Javadoc + PDF) generated successfully."

# # # Generate Javadoc
# # javadoc: $(JAR_FILE)
# # 	@echo "==> Generating Javadoc..."
# # 	@mkdir -p $(DOC_DIR)/html
# # 	# Optionally run the JAR once (ignore errors)
# # 	$(JAVA) -jar $(JAR_FILE) >/dev/null 2>&1 || true
# # 	$(JAVADOC) -d $(DOC_DIR)/html -sourcepath $(SRC_DIR) $(SRC_DIR)/*.java
# # 	@echo "==> Javadoc generated in $(DOC_DIR)/html/"

# # # Compile LaTeX report into PDF
# # pdf:
# # 	@echo "==> Compiling LaTeX report (Part 2)..."
# # 	@cd $(DOC_DIR) && $(LATEX) -interaction=nonstopmode $(REPORT_TEX) >/dev/null
# # 	@cd $(DOC_DIR) && $(LATEX) -interaction=nonstopmode $(REPORT_TEX) >/dev/null
# # 	@echo "==> PDF generated: $(DOC_DIR)/$(REPORT_PDF)"

# # # ==============================================================================
# # # EXECUTION
# # # ==============================================================================

# # # Run parser on a specific .ycc source file
# # run: $(JAR_FILE)
# # 	@if [ -z "$(FILE)" ]; then \
# # 		echo "Error: you must specify a file."; \
# # 		echo "Usage: make run FILE=test/yourfile.ycc"; \
# # 		exit 1; \
# # 	else \
# # 		echo "==> Running JAR on $(FILE)"; \
# # 		$(JAVA) -jar $(JAR_FILE) "$(FILE)"; \
# # 	fi

# # # Run parser on all test files inside the test directory
# # runTest: $(JAR_FILE)
# # 	@echo "==> Running all tests using JAR..."
# # 	@for file in $(TEST_DIR)/*.ycc; do \
# # 		echo ""; \
# # 		echo "---------- $$file ----------"; \
# # 		$(JAVA) -jar $(JAR_FILE) "$$file"; \
# # 	done
# # 	@echo ""
# # 	@echo "==> All tests completed."

# # # Run parser with -wt option to generate LaTeX parse tree + PDF
# # # Example:
# # #   make runWT FILE=test/Euclid.ycc
# # # Generates:
# # #   more/Euclid.tex  +  more/Euclid.pdf  (without .aux/.log/.out)

# # runWT: $(JAR_FILE)
# # 	@if [ -z "$(FILE)" ]; then \
# # 	  echo "Error: you must specify a file."; \
# # 	  echo "Usage: make runWT FILE=test/Euclid.ycc"; \
# # 	  exit 1; \
# # 	else \
# # 	  mkdir -p $(MORE_DIR); \
# # 	  BASENAME=$$(basename "$(FILE)"); \
# # 	  OUTPUT="$(MORE_DIR)/$${BASENAME%.*}.tex"; \
# # 	  echo "==> Running JAR on $(FILE) with -wt, output: $$OUTPUT"; \
# # 	  $(JAVA) -jar $(JAR_FILE) -wt "$$OUTPUT" "$(FILE)"; \
# # 	  echo "==> Compiling $$OUTPUT to PDF..."; \
# # 	  $(LATEX) -interaction=nonstopmode -output-directory=$(MORE_DIR) "$$OUTPUT" >/dev/null; \
# # 	  rm -f "$(MORE_DIR)/$${BASENAME%.*}.aux" "$(MORE_DIR)/$${BASENAME%.*}.log" "$(MORE_DIR)/$${BASENAME%.*}.out"; \
# # 	fi

# # # Run parser with -wt on all test/*.ycc files + PDFs
# # # For each test/<name>.ycc:
# # #   more/<name>.tex and more/<name>.pdf are generated,
# # #   without temporary files .aux/.log/.out.
# # runWTTest: $(JAR_FILE)
# # 	@echo "==> Running -wt on all test files..."
# # 	@mkdir -p $(MORE_DIR)
# # 	@for file in $(TEST_DIR)/*.ycc; do \
# # 		BASENAME=$$(basename "$$file"); \
# # 		OUTPUT="$(MORE_DIR)/$${BASENAME%.*}.tex"; \
# # 		echo ""; \
# # 		echo "---------- $$file ----------"; \
# # 		echo "Output: $$OUTPUT"; \
# # 		$(JAVA) -jar $(JAR_FILE) -wt "$$OUTPUT" "$$file"; \
# # 		echo "==> Compiling $$OUTPUT to PDF..."; \
# # 		$(LATEX) -interaction=nonstopmode -output-directory=$(MORE_DIR) "$$OUTPUT" >/dev/null; \
# # 		rm -f "$(MORE_DIR)/$${BASENAME%.*}.aux" \
# # 		      "$(MORE_DIR)/$${BASENAME%.*}.log" \
# # 		      "$(MORE_DIR)/$${BASENAME%.*}.out"; \
# # 	done
# # 	@echo ""
# # 	@echo "==> All -wt tests completed."

# # # ==============================================================================
# # # CLEANUP
# # # ==============================================================================

# # # Clean intermediate build and temp files, but keep the LaTeX report PDF
# # # et les PDF des arbres (more/*.pdf) + fichiers .tex générés.
# # clean:
# # 	@echo "==> Cleaning generated files..."
# # #  	@rm -f $(SRC_DIR)/LexicalAnalyzer.java
# # 	@rm -f $(SRC_DIR)/*.class
# # 	@rm -f $(SRC_DIR)/*~
# # 	@rm -rf $(DIST_DIR)/src/*
# # 	@rm -rf $(MORE_DIR)/*.tex
# # 	@rm -rf $(MORE_DIR)/*.pdf
# # 	# Temporary LaTeX files for the report
# # 	@find $(DOC_DIR) -maxdepth 1 -type f \( -name "*.aux" -o -name "*.log" -o -name "*.out" \) -delete
# # 	# Temporary LaTeX files for the parse trees in more/
# # 	@rm -f $(MORE_DIR)/*.aux $(MORE_DIR)/*.log $(MORE_DIR)/*.out
# # 	# Auto-generated javac argument files
# # 	@rm -f javac.*.args *.args
# # 	@echo "==> Cleanup complete."

# # .PHONY: all doc javadoc pdf run runTest runWT runWTTest clean

# # # Deletes the jar
# # cleanJar:
# # 	@echo "==> Removing JAR files from dist..."
# # 	@find $(DIST_DIR) -type f -name "*.jar" -delete
# # 	@echo "==> JAR cleanup complete."
