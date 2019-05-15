# addresscalc.coffee - local script to calculate base addr based on ancestor indices
#                     uses embedded ainfo id data created by ancestors script 
# ----  ids for addresscalc: ancrow (has rep data by anc), ancrepinput (input tag reps by anc)
#             ancaddr (calculated address), ancreps (number in path) 
#
$ ->
   # apply action to number input elems
   ancrepinputEls = $("#ancrepinput")
   alert('found ' + ancrepinputEls.size() + ' elements')
   ancrepinputEls.change ->
      alert('submitted!')
#   $.each ainfoIdElems, (index, ancinfo) -> 
#      alert('idx = ' + index + ', name=' + ancinfo.data('name'))
 
 
#                  # ---- get the calculated address element and base address          
#                  ancaddrEl = $("#ancaddr").first()
#                  baseaddr = ancaddrEl.data('base')
#                  # ---- for each anc w reps, update path and newaddr
#                  $("#ancrepinput").each (ancidx, anc) ->
#                  anc = ("#ancrepinput").first()
#                  if anc?
#                     maxreps = anc.data('reps')
#                     stride = anc.data('stride')
#                     alert('index=' + ancidx + ', maxreps=' + maxreps + ', stride=' + stride)
#                  newaddr = baseaddr + 64
#                  alert('ancrepinput=' + ("#ancrepinput").size() + ', ancaddr=' + $("#ancaddr").size() + ', ancreps=' + $("#ancreps").size())
#                  # ---- change the calculated address text
#                  ancaddrEl.text('0x' + newaddr.toString(16))
 
         
      