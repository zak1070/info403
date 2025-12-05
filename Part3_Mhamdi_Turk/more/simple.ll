; Target: LLVM IR
declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
@.str_out = private unnamed_addr constant [4 x i8] c"%d\0A\00"
@.str_in = private unnamed_addr constant [3 x i8] c"%d\00"
@.str_prompt = private unnamed_addr constant [9 x i8] c"input : \00"

define i32 @main() {
entry:
  ; Variable allocations
  %x = alloca i32
  %y = alloca i32
  %z = alloca i32
  ; End allocations

  store i32 10, i32* %x
  store i32 5, i32* %y
  %1 = load i32, i32* %x
  %2 = load i32, i32* %y
  %3 = add i32 %1, %2
  store i32 %3, i32* %z
  %4 = load i32, i32* %z
  %5 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str_out, i32 0, i32 0), i32 %4)
  ret i32 0
}
