module test_memory(
    input wire clk,
    input wire rst,
    output wire[31:0] data,
    output wire[19:0] addr,
    output wire ram_ce_n,
    output wire mem_write,
    output wire mem_read,
    output wire clk_out,
    output wire ready
);

integer count = 0;
reg[1:0] state = 'b00;
reg[31:0] reg_data = 32'b0;
reg[19:0] reg_addr = 'h08000;
reg reg_ce,reg_write,reg_read = 'b1;
reg finish = 'b0;
wire clk_3 = 'b0;
integer count_clk = 0;


assign ready = finish;
assign data = reg_data;
assign addr = reg_addr;
assign ram_ce_n = reg_ce;
assign mem_write = reg_write;
assign mem_read = reg_read;
assign clk_out = (finish) ? 'b0 : clk;
assign clk_3 = ((count_clk == 0) && (!finish)) ? 'b1 : 'b0;

always@(posedge clk)
begin
    count_clk <= count_clk + 1;
    if (count_clk == 3)
        count_clk <= 0;
end

always@(posedge clk_3 or posedge rst)
begin
    if (rst) begin
        count <= 0;
        state <= 'b00;
        reg_data <= 32'b0;
        reg_addr <= 'h08000;
        reg_ce <= 'b1;
        reg_write <= 'b1;
        reg_read <= 'b1;
        finish <= 'b0;
    end
    else begin
        case (state)
            'b00: begin
                count <= count + 1;
                reg_ce <= 'b0;
                reg_write <= 'b0;
                reg_data <= reg_data + 'b1;
                reg_addr <= reg_addr + 'b1;
                if (count == 10) begin
                    count <= 0;
                    state <= 'b01;
                end
            end

            'b01: begin
                count <= count + 1;
                reg_ce <= 'b1;
                reg_write <= 'b0;
                reg_data <= reg_data + 'b1;
                reg_addr <= reg_addr + 'b1;
                if (count == 10) begin
                    count <= 0;
                    state <= 'b11;
                    finish <= 'b1;
                end
            end
            default: begin
                count <= 0;
                reg_data <= 32'b0;
                reg_addr <= 'h08000;
                reg_ce <= 'b1;
                reg_write <= 'b1;
                reg_read <= 'b1;
                finish <= 'b1;
            end
        endcase
    end


end

endmodule