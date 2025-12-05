; Target: LLVM IR
declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
@.str_out = private unnamed_addr constant [4 x i8] c"%d\0A\00"
@.str_in = private unnamed_addr constant [3 x i8] c"%d\00"
@.str_prompt = private unnamed_addr constant [9 x i8] c"input : \00"

define i32 @main() {
entry:
  ; Variable allocations
  %a = alloca i32
  ; End allocations

  %1 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([9 x i8], [9 x i8]* @.str_prompt, i32 0, i32 0))
  %2 = call i32 (i8*, ...) @scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @.str_in, i32 0, i32 0), i32* %a)
  %3 = load i32, i32* %a
  %4 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str_out, i32 0, i32 0), i32 %3)
  ret i32 0
}
